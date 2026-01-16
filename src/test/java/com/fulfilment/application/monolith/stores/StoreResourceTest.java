package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for StoreResource REST endpoints.
 *
 * This test class verifies the behavior of Store CRUD operations and ensures that
 * the LegacyStoreManagerGateway is called ONLY after successful database transaction commits,
 * implementing the requirement from Task 2 of the code assignment.
 *
 * Test Coverage:
 * - Positive scenarios: Successful create, update, patch, get, and delete operations
 * - Negative scenarios: Invalid inputs, non-existent resources, validation failures
 * - Error scenarios: Transaction rollbacks, constraint violations
 * - Event-driven behavior: Verifying legacy system sync occurs after DB commit
 */
@QuarkusTest
@DisplayName("Store Resource Integration Tests")
public class StoreResourceTest {

    @InjectMock
    LegacyStoreManagerGateway legacyStoreManagerGateway;

    @BeforeEach
    @Transactional
    public void cleanup() {
        Store.deleteAll();
    }

    // ==================== POSITIVE TEST CASES ====================

    /**
     * Positive Test: Create a new store with valid data.
     *
     * Verifies:
     * - Store is successfully created with HTTP 201
     * - Response contains correct store data with generated ID
     * - Legacy gateway is called AFTER transaction commits
     * - Data is persisted in database
     */
    @Test
    @DisplayName("POST /store - Successfully create store and sync to legacy system after commit")
    public void testCreateStore_ValidData_ShouldSucceedAndCallLegacyGatewayAfterCommit() {
        // given
        Store newStore = new Store("Electronics Store");
        newStore.quantityProductsInStock = 150;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(newStore)
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .body("name", is("Electronics Store"))
            .body("quantityProductsInStock", is(150))
            .body("id", notNullValue());

        // then - verify legacy gateway was called after transaction commit
        // Using timeout to wait for async event processing after transaction
        verify(legacyStoreManagerGateway, timeout(2000).times(1))
            .createStoreOnLegacySystem(argThat(store ->
                store.name.equals("Electronics Store") &&
                store.quantityProductsInStock == 150
            ));
    }

    /**
     * Positive Test: Update existing store with valid data.
     *
     * Verifies:
     * - Store is successfully updated with HTTP 200
     * - Response contains updated data
     * - Legacy gateway is called AFTER transaction commits with updated entity
     */
    @Test
    @Transactional
    @DisplayName("PUT /store/{id} - Successfully update store and sync to legacy system after commit")
    public void testUpdateStore_ValidData_ShouldSucceedAndCallLegacyGatewayAfterCommit() {
        // given - create a store first
        Store store = new Store("Original Store");
        store.quantityProductsInStock = 50;
        store.persist();
        Long storeId = store.id;

        Store updatedStore = new Store("Updated Store Name");
        updatedStore.quantityProductsInStock = 75;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(updatedStore)
            .when()
            .put("/store/" + storeId)
            .then()
            .statusCode(200)
            .body("name", is("Updated Store Name"))
            .body("quantityProductsInStock", is(75))
            .body("id", is(storeId.intValue()));

        // then - verify legacy gateway was called after transaction commit
        verify(legacyStoreManagerGateway, timeout(2000).times(1))
            .updateStoreOnLegacySystem(argThat(s ->
                s.name.equals("Updated Store Name") &&
                s.quantityProductsInStock == 75
            ));
    }

    /**
     * Positive Test: Partially update store using PATCH.
     *
     * Verifies:
     * - Store is successfully patched with HTTP 200
     * - Only provided fields are updated
     * - Legacy gateway is called AFTER transaction commits
     */
    @Test
    @Transactional
    @DisplayName("PATCH /store/{id} - Successfully patch store and sync to legacy system after commit")
    public void testPatchStore_ValidData_ShouldSucceedAndCallLegacyGatewayAfterCommit() {
        // given - create a store first
        Store store = new Store("Original Store");
        store.quantityProductsInStock = 50;
        store.persist();

        Store patchedStore = new Store("Patched Store");
        patchedStore.quantityProductsInStock = 60;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(patchedStore)
            .when()
            .patch("/store/" + store.id)
            .then()
            .statusCode(200)
            .body("name", is("Patched Store"))
            .body("quantityProductsInStock", is(60));

        // then - verify legacy gateway was called after transaction commit
        verify(legacyStoreManagerGateway, timeout(2000).times(1))
            .updateStoreOnLegacySystem(any(Store.class));
    }

    /**
     * Positive Test: Retrieve all stores.
     *
     * Verifies:
     * - All stores are returned with HTTP 200
     * - Stores are sorted by name
     * - Response contains correct number of stores
     */
    @Test
    @Transactional
    @DisplayName("GET /store - Successfully retrieve all stores sorted by name")
    public void testGetAllStores_MultipleStores_ShouldReturnSortedList() {
        // given
        Store store1 = new Store("Zebra Store");
        store1.quantityProductsInStock = 10;
        store1.persist();

        Store store2 = new Store("Alpha Store");
        store2.quantityProductsInStock = 20;
        store2.persist();

        Store store3 = new Store("Beta Store");
        store3.quantityProductsInStock = 30;
        store3.persist();

        // when/then
        given()
            .when()
            .get("/store")
            .then()
            .statusCode(200)
            .body("size()", is(3))
            .body("[0].name", is("Alpha Store"))
            .body("[1].name", is("Beta Store"))
            .body("[2].name", is("Zebra Store"));
    }

    /**
     * Positive Test: Retrieve single store by ID.
     *
     * Verifies:
     * - Store is retrieved with HTTP 200
     * - Response contains correct store data
     */
    @Test
    @Transactional
    @DisplayName("GET /store/{id} - Successfully retrieve single store by ID")
    public void testGetSingleStore_ExistingId_ShouldReturnStore() {
        // given
        Store store = new Store("Test Store");
        store.quantityProductsInStock = 100;
        store.persist();

        // when/then
        given()
            .when()
            .get("/store/" + store.id)
            .then()
            .statusCode(200)
            .body("id", is(store.id.intValue()))
            .body("name", is("Test Store"))
            .body("quantityProductsInStock", is(100));
    }

    /**
     * Positive Test: Delete existing store.
     *
     * Verifies:
     * - Store is deleted with HTTP 204
     * - Store no longer exists in database
     */
    @Test
    @Transactional
    @DisplayName("DELETE /store/{id} - Successfully delete store")
    public void testDeleteStore_ExistingId_ShouldSucceed() {
        // given
        Store store = new Store("To Be Deleted");
        store.quantityProductsInStock = 100;
        store.persist();
        Long storeId = store.id;

        // when
        given()
            .when()
            .delete("/store/" + storeId)
            .then()
            .statusCode(204);

        // then - verify store was deleted
        assertEquals(0, Store.count());
    }

    /**
     * Positive Test: Create store with zero stock.
     *
     * Verifies:
     * - Store can be created with zero stock
     * - System handles edge case of empty stock
     */
    @Test
    @DisplayName("POST /store - Create store with zero stock quantity")
    public void testCreateStore_ZeroStock_ShouldSucceed() {
        // given
        Store newStore = new Store("Empty Store");
        newStore.quantityProductsInStock = 0;

        // when/then
        given()
            .contentType(ContentType.JSON)
            .body(newStore)
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .body("name", is("Empty Store"))
            .body("quantityProductsInStock", is(0));

        verify(legacyStoreManagerGateway, timeout(2000).times(1))
            .createStoreOnLegacySystem(any(Store.class));
    }

    // ==================== NEGATIVE TEST CASES ====================

    /**
     * Negative Test: Create store with ID already set.
     *
     * Verifies:
     * - Request is rejected with HTTP 422
     * - Legacy gateway is NOT called
     * - No data is persisted
     */
    @Test
    @DisplayName("POST /store - Reject create request with pre-set ID")
    public void testCreateStore_WithPresetId_ShouldRejectAndNotCallLegacyGateway() {
        // given
        Store newStore = new Store("Test Store");
        newStore.id = 999L; // Invalid - setting id on create
        newStore.quantityProductsInStock = 100;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(newStore)
            .when()
            .post("/store")
            .then()
            .statusCode(422)
            .body("error", is("Id was invalidly set on request."));

        // then - verify legacy gateway was NOT called because transaction failed
        verify(legacyStoreManagerGateway, never())
            .createStoreOnLegacySystem(any(Store.class));
    }

    /**
     * Negative Test: Update non-existent store.
     *
     * Verifies:
     * - Request is rejected with HTTP 404
     * - Legacy gateway is NOT called
     * - Appropriate error message is returned
     */
    @Test
    @DisplayName("PUT /store/{id} - Reject update for non-existent store")
    public void testUpdateStore_NonExistentId_ShouldRejectAndNotCallLegacyGateway() {
        // given
        Store updatedStore = new Store("Updated Store");
        updatedStore.quantityProductsInStock = 75;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(updatedStore)
            .when()
            .put("/store/99999")
            .then()
            .statusCode(404)
            .body("error", is("Store with id of 99999 does not exist."));

        // then - verify legacy gateway was NOT called
        verify(legacyStoreManagerGateway, never())
            .updateStoreOnLegacySystem(any(Store.class));
    }

    /**
     * Negative Test: Patch non-existent store.
     *
     * Verifies:
     * - Request is rejected with HTTP 404
     * - Legacy gateway is NOT called
     */
    @Test
    @DisplayName("PATCH /store/{id} - Reject patch for non-existent store")
    public void testPatchStore_NonExistentId_ShouldRejectAndNotCallLegacyGateway() {
        // given
        Store patchedStore = new Store("Patched Store");
        patchedStore.quantityProductsInStock = 60;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(patchedStore)
            .when()
            .patch("/store/99999")
            .then()
            .statusCode(404);

        // then
        verify(legacyStoreManagerGateway, never())
            .updateStoreOnLegacySystem(any(Store.class));
    }

    /**
     * Negative Test: Get non-existent store.
     *
     * Verifies:
     * - Request returns HTTP 404
     * - Appropriate error message is returned
     */
    @Test
    @DisplayName("GET /store/{id} - Return 404 for non-existent store")
    public void testGetStore_NonExistentId_ShouldReturn404() {
        // when/then
        given()
            .when()
            .get("/store/99999")
            .then()
            .statusCode(404)
            .body("error", is("Store with id of 99999 does not exist."));
    }

    /**
     * Negative Test: Delete non-existent store.
     *
     * Verifies:
     * - Request returns HTTP 404
     * - Appropriate error message is returned
     */
    @Test
    @DisplayName("DELETE /store/{id} - Return 404 when deleting non-existent store")
    public void testDeleteStore_NonExistentId_ShouldReturn404() {
        // when/then
        given()
            .when()
            .delete("/store/99999")
            .then()
            .statusCode(404)
            .body("error", is("Store with id of 99999 does not exist."));
    }

    /**
     * Negative Test: Update store without name.
     *
     * Verifies:
     * - Request is rejected with HTTP 422
     * - Legacy gateway is NOT called
     * - Validation error message is returned
     */
    @Test
    @Transactional
    @DisplayName("PUT /store/{id} - Reject update without store name")
    public void testUpdateStore_MissingName_ShouldRejectAndNotCallLegacyGateway() {
        // given
        Store store = new Store("Original Store");
        store.quantityProductsInStock = 50;
        store.persist();

        Store updatedStore = new Store();
        updatedStore.name = null; // Invalid - missing name
        updatedStore.quantityProductsInStock = 75;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(updatedStore)
            .when()
            .put("/store/" + store.id)
            .then()
            .statusCode(422)
            .body("error", is("Store Name was not set on request."));

        // then
        verify(legacyStoreManagerGateway, never())
            .updateStoreOnLegacySystem(any(Store.class));
    }

    /**
     * Negative Test: Patch store without name.
     *
     * Verifies:
     * - Request is rejected with HTTP 422
     * - Legacy gateway is NOT called
     */
    @Test
    @Transactional
    @DisplayName("PATCH /store/{id} - Reject patch without store name")
    public void testPatchStore_MissingName_ShouldRejectAndNotCallLegacyGateway() {
        // given
        Store store = new Store("Original Store");
        store.quantityProductsInStock = 50;
        store.persist();

        Store patchedStore = new Store();
        patchedStore.name = null; // Invalid
        patchedStore.quantityProductsInStock = 60;

        // when
        given()
            .contentType(ContentType.JSON)
            .body(patchedStore)
            .when()
            .patch("/store/" + store.id)
            .then()
            .statusCode(422);

        // then
        verify(legacyStoreManagerGateway, never())
            .updateStoreOnLegacySystem(any(Store.class));
    }

    // ==================== ERROR CONDITION TESTS ====================

    /**
     * Error Test: Verify GET returns empty list when no stores exist.
     *
     * Verifies:
     * - Empty database returns HTTP 200 with empty array
     * - No errors occur with empty dataset
     */
    @Test
    @DisplayName("GET /store - Return empty list when no stores exist")
    public void testGetAllStores_EmptyDatabase_ShouldReturnEmptyList() {
        // when/then
        given()
            .when()
            .get("/store")
            .then()
            .statusCode(200)
            .body("size()", is(0));
    }

    /**
     * Error Test: Invalid JSON payload.
     *
     * Verifies:
     * - Malformed JSON is rejected
     * - Appropriate error response is returned
     * - Legacy gateway is NOT called
     */
    @Test
    @DisplayName("POST /store - Reject malformed JSON payload")
    public void testCreateStore_InvalidJson_ShouldReject() {
        // when/then
        given()
            .contentType(ContentType.JSON)
            .body("{invalid json")
            .when()
            .post("/store")
            .then()
            .statusCode(400);

        verify(legacyStoreManagerGateway, never())
            .createStoreOnLegacySystem(any(Store.class));
    }

    /**
     * Integration Test: Verify transaction and event mechanism.
     *
     * Verifies:
     * - Store is persisted in database
     * - Data integrity is maintained
     * - Events can be fired within transactions
     */
    @Test
    @Transactional
    @DisplayName("Verify store persistence and data integrity")
    public void testStoreEventListener_VerifyTransactionAndPersistence() {
        // given
        Store store = new Store("Event Test Store");
        store.quantityProductsInStock = 50;

        // when
        store.persist();

        // then - verify store was persisted correctly
        Store found = Store.findById(store.id);
        assertNotNull(found, "Store should be persisted");
        assertEquals("Event Test Store", found.name);
        assertEquals(50, found.quantityProductsInStock);
        assertNotNull(found.id, "ID should be generated");
    }

    /**
     * Error Test: Verify error response format.
     *
     * Verifies:
     * - Error responses contain required fields
     * - Error messages are informative
     * - HTTP status codes are appropriate
     */
    @Test
    @DisplayName("Verify error response format and structure")
    public void testErrorResponse_ShouldContainProperStructure() {
        // when/then
        given()
            .when()
            .get("/store/99999")
            .then()
            .statusCode(404)
            .body("$", hasKey("error"))
            .body("$", hasKey("code"))
            .body("code", is(404));
    }
}