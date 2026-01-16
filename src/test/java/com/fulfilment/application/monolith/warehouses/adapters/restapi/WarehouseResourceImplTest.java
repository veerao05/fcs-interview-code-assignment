package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WarehouseResourceImpl REST endpoints.
 *
 * <p>This test class verifies the behavior of Warehouse CRUD operations, ensuring proper handling
 * of:
 *
 * <ul>
 *   <li>Warehouse creation with all validation rules
 *   <li>Warehouse retrieval by business unit code
 *   <li>Warehouse archiving
 *   <li>Warehouse replacement with stock and capacity validations
 *   <li>Proper HTTP status codes and error responses
 * </ul>
 *
 * <p>Test Coverage:
 *
 * <ul>
 *   <li>Positive scenarios: Successful CRUD operations
 *   <li>Negative scenarios: Invalid inputs, validation failures
 *   <li>Error scenarios: Not found errors, business rule violations
 * </ul>
 */
@QuarkusTest
@DisplayName("Warehouse Resource Integration Tests")
public class WarehouseResourceImplTest {

  @Inject WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  public void cleanup() {
    warehouseRepository.deleteAll();
  }

  // ==================== LIST ALL WAREHOUSES ====================

  /**
   * Positive Test: List all warehouses when database is empty.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 200
   *   <li>Returns empty array
   * </ul>
   */
  @Test
  @DisplayName("GET /warehouse - Successfully list empty warehouses")
  public void testListAllWarehouses_EmptyDatabase_ShouldReturnEmptyArray() {
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  /**
   * Positive Test: List all warehouses with existing data.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 200
   *   <li>Returns array with all warehouses
   *   <li>Each warehouse has all required fields
   * </ul>
   */
  @Test
  @DisplayName("GET /warehouse - Successfully list all warehouses")
  @Transactional
  public void testListAllWarehouses_WithData_ShouldReturnAllWarehouses() {
    // Given - create test warehouses
    DbWarehouse warehouse1 = new DbWarehouse();
    warehouse1.businessUnitCode = "BU001";
    warehouse1.location = "NYC";
    warehouse1.capacity = 1000;
    warehouse1.stock = 500;
    warehouse1.createdAt = LocalDateTime.now();
    warehouse1.persist();

    DbWarehouse warehouse2 = new DbWarehouse();
    warehouse2.businessUnitCode = "BU002";
    warehouse2.location = "LA";
    warehouse2.capacity = 2000;
    warehouse2.stock = 1000;
    warehouse2.createdAt = LocalDateTime.now();
    warehouse2.persist();

    // When & Then
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body("$", hasSize(2))
        .body("[0].businessUnitCode", notNullValue())
        .body("[0].location", notNullValue())
        .body("[0].capacity", notNullValue())
        .body("[0].stock", notNullValue());
  }

  // ==================== CREATE WAREHOUSE ====================

  /**
   * Positive Test: Create a new warehouse with valid data.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Warehouse is successfully created with HTTP 201
   *   <li>Response contains correct warehouse data
   *   <li>Data is persisted in database
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse - Successfully create warehouse with valid data")
  public void testCreateWarehouse_ValidData_ShouldSucceed() {
    // Given
    String requestBody =
        """
        {
          "businessUnitCode": "BU_NEW_001",
          "location": "AMSTERDAM-001",
          "capacity": 1000,
          "stock": 500
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(201)
        .body("businessUnitCode", is("BU_NEW_001"))
        .body("location", is("AMSTERDAM-001"))
        .body("capacity", is(1000))
        .body("stock", is(500));
  }

  /**
   * Positive Test: Create warehouse with zero stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Warehouse with zero stock can be created
   *   <li>Returns HTTP 201
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse - Successfully create warehouse with zero stock")
  public void testCreateWarehouse_ZeroStock_ShouldSucceed() {
    // Given
    String requestBody =
        """
        {
          "businessUnitCode": "BU_ZERO_STOCK",
          "location": "AMSTERDAM-001",
          "capacity": 1000,
          "stock": 0
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(201)
        .body("stock", is(0));
  }

  /**
   * Negative Test: Create warehouse with duplicate business unit code.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates duplicate business unit code
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse - Fail when business unit code already exists")
  @Transactional
  public void testCreateWarehouse_DuplicateBusinessUnitCode_ShouldFail() {
    // Given - create existing warehouse
    DbWarehouse existing = new DbWarehouse();
    existing.businessUnitCode = "BU_DUPLICATE";
    existing.location = "AMSTERDAM-001";
    existing.capacity = 1000;
    existing.stock = 500;
    existing.createdAt = LocalDateTime.now();
    existing.persist();

    String requestBody =
        """
        {
          "businessUnitCode": "BU_DUPLICATE",
          "location": "AMSTERDAM-001",
          "capacity": 2000,
          "stock": 1000
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }

  /**
   * Negative Test: Create warehouse with invalid location.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates invalid location
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse - Fail when location is invalid")
  public void testCreateWarehouse_InvalidLocation_ShouldFail() {
    // Given
    String requestBody =
        """
        {
          "businessUnitCode": "BU_INVALID_LOC",
          "location": "INVALID_LOCATION",
          "capacity": 1000,
          "stock": 500
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }

  /**
   * Negative Test: Create warehouse with zero capacity.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates capacity must be greater than zero
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse - Fail when capacity is zero")
  public void testCreateWarehouse_ZeroCapacity_ShouldFail() {
    // Given
    String requestBody =
        """
        {
          "businessUnitCode": "BU_ZERO_CAP",
          "location": "AMSTERDAM-001",
          "capacity": 0,
          "stock": 0
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }

  /**
   * Negative Test: Create warehouse with stock exceeding capacity.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates stock cannot exceed capacity
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse - Fail when stock exceeds capacity")
  public void testCreateWarehouse_StockExceedsCapacity_ShouldFail() {
    // Given
    String requestBody =
        """
        {
          "businessUnitCode": "BU_STOCK_EXCEED",
          "location": "AMSTERDAM-001",
          "capacity": 1000,
          "stock": 1500
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }

  // ==================== GET WAREHOUSE BY ID ====================

  /**
   * Positive Test: Get warehouse by business unit code.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 200
   *   <li>Response contains correct warehouse data
   * </ul>
   */
  @Test
  @DisplayName("GET /warehouse/{id} - Successfully get warehouse by business unit code")
  @Transactional
  public void testGetWarehouseById_ValidId_ShouldSucceed() {
    // Given - create test warehouse
    DbWarehouse warehouse = new DbWarehouse();
    warehouse.businessUnitCode = "BU_GET_TEST";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = 500;
    warehouse.createdAt = LocalDateTime.now();
    warehouse.persist();

    // When & Then
    given()
        .when()
        .get("/warehouse/BU_GET_TEST")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("BU_GET_TEST"))
        .body("location", is("NYC"))
        .body("capacity", is(1000))
        .body("stock", is(500));
  }

  /**
   * Negative Test: Get warehouse with non-existent ID.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 404
   * </ul>
   */
  @Test
  @DisplayName("GET /warehouse/{id} - Fail when warehouse not found")
  public void testGetWarehouseById_NotFound_ShouldFail() {
    // When & Then
    given()
        .when()
        .get("/warehouse/NONEXISTENT_BU")
        .then()
        .statusCode(404);
  }

  // ==================== ARCHIVE WAREHOUSE ====================

  /**
   * Positive Test: Archive an existing warehouse.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 204
   *   <li>Warehouse is marked as archived in database
   * </ul>
   */
  @Test
  @DisplayName("DELETE /warehouse/{id} - Successfully archive warehouse")
  @Transactional
  public void testArchiveWarehouse_ValidId_ShouldSucceed() {
    // Given - create test warehouse
    DbWarehouse warehouse = new DbWarehouse();
    warehouse.businessUnitCode = "BU_ARCHIVE_TEST";
    warehouse.location = "LA";
    warehouse.capacity = 1500;
    warehouse.stock = 750;
    warehouse.createdAt = LocalDateTime.now();
    warehouse.persist();

    // When & Then
    given()
        .when()
        .delete("/warehouse/BU_ARCHIVE_TEST")
        .then()
        .statusCode(204);

    // Verify warehouse is archived
    DbWarehouse archived = warehouseRepository.findByBusinessUnitCode("BU_ARCHIVE_TEST");
    assert archived != null;
    assert archived.archivedAt != null;
  }

  /**
   * Negative Test: Archive non-existent warehouse.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 404
   * </ul>
   */
  @Test
  @DisplayName("DELETE /warehouse/{id} - Fail when warehouse not found")
  public void testArchiveWarehouse_NotFound_ShouldFail() {
    // When & Then
    given()
        .when()
        .delete("/warehouse/NONEXISTENT_BU")
        .then()
        .statusCode(404);
  }

  /**
   * Negative Test: Archive already archived warehouse.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates warehouse is already archived
   * </ul>
   */
  @Test
  @DisplayName("DELETE /warehouse/{id} - Fail when warehouse already archived")
  @Transactional
  public void testArchiveWarehouse_AlreadyArchived_ShouldFail() {
    // Given - create archived warehouse
    DbWarehouse warehouse = new DbWarehouse();
    warehouse.businessUnitCode = "BU_ALREADY_ARCHIVED";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = 500;
    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = LocalDateTime.now(); // Already archived
    warehouse.persist();

    // When & Then
    given()
        .when()
        .delete("/warehouse/BU_ALREADY_ARCHIVED")
        .then()
        .statusCode(400);
  }

  // ==================== REPLACE WAREHOUSE ====================

  /**
   * Positive Test: Replace warehouse with valid data.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 200
   *   <li>Response contains updated warehouse data
   *   <li>Business unit code remains the same
   *   <li>Location, capacity, and stock are updated
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse/{businessUnitCode}/replacement - Successfully replace warehouse")
  @Transactional
  public void testReplaceWarehouse_ValidData_ShouldSucceed() {
    // Given - create existing warehouse
    DbWarehouse existing = new DbWarehouse();
    existing.businessUnitCode = "BU_REPLACE_TEST";
    existing.location = "NYC";
    existing.capacity = 1000;
    existing.stock = 500;
    existing.createdAt = LocalDateTime.now();
    existing.persist();

    String requestBody =
        """
        {
          "businessUnitCode": "BU_REPLACE_TEST",
          "location": "LA",
          "capacity": 1500,
          "stock": 500
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse/BU_REPLACE_TEST/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("BU_REPLACE_TEST"))
        .body("location", is("LA"))
        .body("capacity", is(1500))
        .body("stock", is(500));
  }

  /**
   * Negative Test: Replace warehouse with mismatched business unit code.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates business unit code mismatch
   * </ul>
   */
  @Test
  @DisplayName(
      "POST /warehouse/{businessUnitCode}/replacement - Fail when business unit codes don't match")
  @Transactional
  public void testReplaceWarehouse_MismatchedBusinessUnitCode_ShouldFail() {
    // Given - create existing warehouse
    DbWarehouse existing = new DbWarehouse();
    existing.businessUnitCode = "BU_MISMATCH_TEST";
    existing.location = "NYC";
    existing.capacity = 1000;
    existing.stock = 500;
    existing.createdAt = LocalDateTime.now();
    existing.persist();

    String requestBody =
        """
        {
          "businessUnitCode": "BU_DIFFERENT",
          "location": "LA",
          "capacity": 1500,
          "stock": 500
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse/BU_MISMATCH_TEST/replacement")
        .then()
        .statusCode(400);
  }

  /**
   * Negative Test: Replace non-existent warehouse.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates warehouse does not exist
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse/{businessUnitCode}/replacement - Fail when warehouse not found")
  public void testReplaceWarehouse_NotFound_ShouldFail() {
    // Given
    String requestBody =
        """
        {
          "businessUnitCode": "NONEXISTENT_BU",
          "location": "LA",
          "capacity": 1500,
          "stock": 0
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse/NONEXISTENT_BU/replacement")
        .then()
        .statusCode(400);
  }

  /**
   * Negative Test: Replace warehouse with capacity less than current stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates new capacity cannot accommodate existing stock
   * </ul>
   */
  @Test
  @DisplayName(
      "POST /warehouse/{businessUnitCode}/replacement - Fail when new capacity cannot accommodate"
          + " stock")
  @Transactional
  public void testReplaceWarehouse_CapacityTooSmall_ShouldFail() {
    // Given - create existing warehouse with stock
    DbWarehouse existing = new DbWarehouse();
    existing.businessUnitCode = "BU_CAP_TOO_SMALL";
    existing.location = "NYC";
    existing.capacity = 1000;
    existing.stock = 800;
    existing.createdAt = LocalDateTime.now();
    existing.persist();

    String requestBody =
        """
        {
          "businessUnitCode": "BU_CAP_TOO_SMALL",
          "location": "LA",
          "capacity": 700,
          "stock": 800
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse/BU_CAP_TOO_SMALL/replacement")
        .then()
        .statusCode(400);
  }

  /**
   * Negative Test: Replace warehouse with stock not matching old stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates stock must match old warehouse stock
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse/{businessUnitCode}/replacement - Fail when stock doesn't match")
  @Transactional
  public void testReplaceWarehouse_StockMismatch_ShouldFail() {
    // Given - create existing warehouse
    DbWarehouse existing = new DbWarehouse();
    existing.businessUnitCode = "BU_STOCK_MISMATCH";
    existing.location = "NYC";
    existing.capacity = 1000;
    existing.stock = 500;
    existing.createdAt = LocalDateTime.now();
    existing.persist();

    String requestBody =
        """
        {
          "businessUnitCode": "BU_STOCK_MISMATCH",
          "location": "LA",
          "capacity": 1500,
          "stock": 600
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse/BU_STOCK_MISMATCH/replacement")
        .then()
        .statusCode(400);
  }

  /**
   * Negative Test: Replace archived warehouse.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates cannot replace archived warehouse
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse/{businessUnitCode}/replacement - Fail when warehouse is archived")
  @Transactional
  public void testReplaceWarehouse_ArchivedWarehouse_ShouldFail() {
    // Given - create archived warehouse
    DbWarehouse existing = new DbWarehouse();
    existing.businessUnitCode = "BU_ARCHIVED_REPLACE";
    existing.location = "NYC";
    existing.capacity = 1000;
    existing.stock = 500;
    existing.createdAt = LocalDateTime.now();
    existing.archivedAt = LocalDateTime.now(); // Already archived
    existing.persist();

    String requestBody =
        """
        {
          "businessUnitCode": "BU_ARCHIVED_REPLACE",
          "location": "LA",
          "capacity": 1500,
          "stock": 500
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse/BU_ARCHIVED_REPLACE/replacement")
        .then()
        .statusCode(400);
  }

  /**
   * Negative Test: Replace warehouse with invalid location.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   *   <li>Error message indicates location is invalid
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse/{businessUnitCode}/replacement - Fail when location is invalid")
  @Transactional
  public void testReplaceWarehouse_InvalidLocation_ShouldFail() {
    // Given - create existing warehouse
    DbWarehouse existing = new DbWarehouse();
    existing.businessUnitCode = "BU_INVALID_LOC_REPLACE";
    existing.location = "NYC";
    existing.capacity = 1000;
    existing.stock = 500;
    existing.createdAt = LocalDateTime.now();
    existing.persist();

    String requestBody =
        """
        {
          "businessUnitCode": "BU_INVALID_LOC_REPLACE",
          "location": "INVALID_LOCATION",
          "capacity": 1500,
          "stock": 500
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse/BU_INVALID_LOC_REPLACE/replacement")
        .then()
        .statusCode(400);
  }

  // ==================== ERROR/EDGE CASES ====================

  /**
   * Error Test: Create warehouse with missing required fields.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse - Fail when required fields are missing")
  public void testCreateWarehouse_MissingFields_ShouldFail() {
    // Given - missing businessUnitCode
    String requestBody =
        """
        {
          "location": "AMSTERDAM-001",
          "capacity": 1000,
          "stock": 500
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }

  /**
   * Error Test: Create warehouse with malformed JSON.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Returns HTTP 400
   * </ul>
   */
  @Test
  @DisplayName("POST /warehouse - Fail when JSON is malformed")
  public void testCreateWarehouse_MalformedJson_ShouldFail() {
    // Given - malformed JSON
    String requestBody = "{invalid json}";

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }
}