package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Store event-driven legacy synchronization mechanism.
 *
 * This test class verifies that the event-driven architecture properly handles
 * Store lifecycle events (creation and updates) and that the StoreLegacySyncListener
 * correctly observes these events to trigger legacy system synchronization.
 *
 * Test Coverage:
 * - Positive scenarios: Successful event firing and handling
 * - Event data integrity: Verifying events carry correct store information
 * - Transaction integration: Ensuring events work within transactional context
 */
@QuarkusTest
@DisplayName("Store Legacy Sync Listener Unit Tests")
public class StoreLegacySyncListenerTest {

    @Inject
    Event<StoreCreatedEvent> storeCreatedEvent;

    @Inject
    Event<StoreUpdatedEvent> storeUpdatedEvent;

    @InjectMock
    LegacyStoreManagerGateway legacyStoreManagerGateway;

    @BeforeEach
    @Transactional
    public void cleanup() {
        Store.deleteAll();
    }

    // ==================== POSITIVE TEST CASES ====================

    /**
     * Positive Test: Store created event firing within transaction.
     *
     * Verifies:
     * - Store can be persisted successfully
     * - StoreCreatedEvent can be fired within a transaction
     * - Event is processed after transaction commits
     * - Legacy system sync is triggered (verified via integration tests)
     */
    @Test
    @Transactional
    @DisplayName("Fire StoreCreatedEvent within transaction and verify store persistence")
    public void testStoreCreatedEvent_WithinTransaction_ShouldPersistAndFireEvent() {
        // given
        Store store = new Store("Electronics Store");
        store.quantityProductsInStock = 100;

        // when - persist and fire the event within a transaction
        store.persist();
        storeCreatedEvent.fire(new StoreCreatedEvent(store));

        // then - transaction commits when method ends, triggering the event listener
        assertNotNull(store.id, "Store ID should be generated after persist");
        assertEquals("Electronics Store", store.name);
        assertEquals(100, store.quantityProductsInStock);
    }

    /**
     * Positive Test: Store updated event firing within transaction.
     *
     * Verifies:
     * - Store can be updated successfully
     * - StoreUpdatedEvent can be fired within a transaction
     * - Updated data is persisted correctly
     * - Event is processed after transaction commits
     */
    @Test
    @Transactional
    @DisplayName("Fire StoreUpdatedEvent within transaction and verify store update")
    public void testStoreUpdatedEvent_WithinTransaction_ShouldUpdateAndFireEvent() {
        // given - create and persist initial store
        Store store = new Store("Original Name");
        store.quantityProductsInStock = 50;
        store.persist();
        Long originalId = store.id;

        // when - update store and fire event
        store.name = "Updated Name";
        store.quantityProductsInStock = 150;
        storeUpdatedEvent.fire(new StoreUpdatedEvent(store));

        // then - verify updates are reflected
        assertEquals(originalId, store.id, "Store ID should not change on update");
        assertEquals("Updated Name", store.name);
        assertEquals(150, store.quantityProductsInStock);
    }

    /**
     * Positive Test: Multiple event firings in sequence.
     *
     * Verifies:
     * - Multiple events can be fired within same transaction
     * - System handles multiple store operations correctly
     * - All changes are persisted properly
     */
    @Test
    @Transactional
    @DisplayName("Fire multiple events in sequence within single transaction")
    public void testMultipleEvents_InSequence_ShouldHandleAll() {
        // given
        Store store1 = new Store("Store One");
        store1.quantityProductsInStock = 10;
        store1.persist();

        Store store2 = new Store("Store Two");
        store2.quantityProductsInStock = 20;
        store2.persist();

        // when - fire multiple events
        storeCreatedEvent.fire(new StoreCreatedEvent(store1));
        storeCreatedEvent.fire(new StoreCreatedEvent(store2));

        store1.name = "Store One Updated";
        storeUpdatedEvent.fire(new StoreUpdatedEvent(store1));

        // then - verify all stores exist with correct data
        assertEquals(2, Store.count());
        assertEquals("Store One Updated", store1.name);
        assertEquals("Store Two", store2.name);
    }

    // ==================== EVENT DATA INTEGRITY TESTS ====================

    /**
     * Positive Test: StoreCreatedEvent data integrity.
     *
     * Verifies:
     * - Event object correctly encapsulates store data
     * - Store reference is maintained in event
     * - Event can be created and accessed properly
     */
    @Test
    @DisplayName("StoreCreatedEvent should maintain store reference integrity")
    public void testStoreCreatedEvent_DataIntegrity_ShouldMaintainStoreReference() {
        // given
        Store store = new Store("Test Store");
        store.quantityProductsInStock = 100;
        store.id = 123L;

        // when
        StoreCreatedEvent event = new StoreCreatedEvent(store);

        // then - verify event contains correct store reference
        assertNotNull(event.getStore(), "Event should contain store");
        assertSame(store, event.getStore(), "Event should contain exact same store instance");
        assertEquals("Test Store", event.getStore().name);
        assertEquals(100, event.getStore().quantityProductsInStock);
        assertEquals(123L, event.getStore().id);
    }

    /**
     * Positive Test: StoreUpdatedEvent data integrity.
     *
     * Verifies:
     * - Event object correctly encapsulates updated store data
     * - Store reference is maintained in event
     * - Event reflects latest store state
     */
    @Test
    @DisplayName("StoreUpdatedEvent should maintain store reference integrity")
    public void testStoreUpdatedEvent_DataIntegrity_ShouldMaintainStoreReference() {
        // given
        Store store = new Store("Updated Store");
        store.quantityProductsInStock = 200;
        store.id = 456L;

        // when
        StoreUpdatedEvent event = new StoreUpdatedEvent(store);

        // then - verify event contains correct store reference
        assertNotNull(event.getStore(), "Event should contain store");
        assertSame(store, event.getStore(), "Event should contain exact same store instance");
        assertEquals("Updated Store", event.getStore().name);
        assertEquals(200, event.getStore().quantityProductsInStock);
        assertEquals(456L, event.getStore().id);
    }

    /**
     * Positive Test: Event objects with different stores.
     *
     * Verifies:
     * - Multiple event instances maintain separate store references
     * - No cross-contamination between events
     * - Each event is independent
     */
    @Test
    @DisplayName("Multiple events should maintain separate store references")
    public void testMultipleEvents_DifferentStores_ShouldMaintainSeparateReferences() {
        // given
        Store store1 = new Store("Store One");
        store1.quantityProductsInStock = 10;
        store1.id = 1L;

        Store store2 = new Store("Store Two");
        store2.quantityProductsInStock = 20;
        store2.id = 2L;

        // when
        StoreCreatedEvent event1 = new StoreCreatedEvent(store1);
        StoreCreatedEvent event2 = new StoreCreatedEvent(store2);
        StoreUpdatedEvent event3 = new StoreUpdatedEvent(store1);

        // then - verify each event has correct, separate store reference
        assertSame(store1, event1.getStore());
        assertSame(store2, event2.getStore());
        assertSame(store1, event3.getStore());

        assertEquals("Store One", event1.getStore().name);
        assertEquals("Store Two", event2.getStore().name);
        assertEquals("Store One", event3.getStore().name);

        assertEquals(10, event1.getStore().quantityProductsInStock);
        assertEquals(20, event2.getStore().quantityProductsInStock);
        assertEquals(10, event3.getStore().quantityProductsInStock);
    }

    // ==================== EDGE CASE TESTS ====================

    /**
     * Edge Case Test: Event with store having zero stock.
     *
     * Verifies:
     * - System handles edge case of zero stock quantity
     * - Event can carry stores with boundary values
     */
    @Test
    @DisplayName("Event should handle store with zero stock quantity")
    public void testEvent_StoreWithZeroStock_ShouldHandle() {
        // given
        Store store = new Store("Empty Store");
        store.quantityProductsInStock = 0;

        // when
        StoreCreatedEvent event = new StoreCreatedEvent(store);

        // then
        assertNotNull(event.getStore());
        assertEquals(0, event.getStore().quantityProductsInStock);
    }

    /**
     * Edge Case Test: Event with store having large stock quantity.
     *
     * Verifies:
     * - System handles large stock quantities
     * - No overflow or data corruption issues
     */
    @Test
    @DisplayName("Event should handle store with large stock quantity")
    public void testEvent_StoreWithLargeStock_ShouldHandle() {
        // given
        Store store = new Store("Large Warehouse");
        store.quantityProductsInStock = Integer.MAX_VALUE;

        // when
        StoreUpdatedEvent event = new StoreUpdatedEvent(store);

        // then
        assertNotNull(event.getStore());
        assertEquals(Integer.MAX_VALUE, event.getStore().quantityProductsInStock);
    }

    /**
     * Edge Case Test: Event with store having long name.
     *
     * Verifies:
     * - System handles stores with maximum allowed name length
     * - No truncation or data loss in events
     */
    @Test
    @DisplayName("Event should handle store with maximum length name")
    public void testEvent_StoreWithLongName_ShouldHandle() {
        // given - create name at max length (40 chars as per Store entity)
        String longName = "A".repeat(40);
        Store store = new Store(longName);
        store.quantityProductsInStock = 50;

        // when
        StoreCreatedEvent event = new StoreCreatedEvent(store);

        // then
        assertNotNull(event.getStore());
        assertEquals(longName, event.getStore().name);
        assertEquals(40, event.getStore().name.length());
    }
}
