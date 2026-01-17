package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.junit.QuarkusTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Comprehensive test suite for {@link ReplaceWarehouseUseCase}.
 *
 * <p>This test suite validates the warehouse replacement functionality including:
 *
 * <ul>
 *   <li>Positive scenarios: Successful warehouse replacement with valid data
 *   <li>Negative scenarios: Validation failures for business rules
 *   <li>Error scenarios: Edge cases and boundary conditions
 * </ul>
 *
 * <p>The tests ensure proper enforcement of business rules:
 *
 * <ul>
 *   <li>Warehouse must exist before replacement
 *   <li>Cannot replace an archived warehouse
 *   <li>Location must be valid
 *   <li>Capacity must be greater than zero
 *   <li>New capacity must accommodate existing stock
 *   <li>New stock must match old stock
 * </ul>
 */
@QuarkusTest
@DisplayName("ReplaceWarehouseUseCase Tests")
class ReplaceWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @BeforeEach
  void setUp() {
    warehouseStore = Mockito.mock(WarehouseStore.class);
    locationResolver = Mockito.mock(LocationResolver.class);
    replaceWarehouseUseCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  // ==================== Positive Test Cases ====================

  /**
   * Tests successful warehouse replacement with valid data.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Warehouse is successfully replaced when all validations pass
   *   <li>Location, capacity, and stock are updated correctly
   *   <li>Business unit code remains the same for cost history preservation
   *   <li>createdAt timestamp is updated to mark replacement time
   *   <li>archivedAt is set to null to ensure active status
   * </ul>
   */
  @Test
  @DisplayName("Should successfully replace warehouse with valid data")
  void testReplaceWarehouse_Success() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU001";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 500;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU001";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 1500;
    newWarehouse.stock = 500;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU001")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When
    replaceWarehouseUseCase.replace(newWarehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse updatedWarehouse = warehouseCaptor.getValue();
    assertEquals("BU001", updatedWarehouse.businessUnitCode);
    assertEquals("LA", updatedWarehouse.location);
    assertEquals(1500, updatedWarehouse.capacity);
    assertEquals(500, updatedWarehouse.stock);
    assertNotNull(updatedWarehouse.createdAt);
    assertNull(updatedWarehouse.archivedAt);
  }

  /**
   * Tests warehouse replacement with zero stock in both old and new warehouse.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Replacement succeeds when both warehouses have zero stock
   *   <li>Stock matching validation passes for zero values
   *   <li>Capacity accommodation is satisfied (any capacity >= 0)
   * </ul>
   */
  @Test
  @DisplayName("Should replace warehouse when both old and new have zero stock")
  void testReplaceWarehouse_ZeroStock() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU002";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 0;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU002";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 800;
    newWarehouse.stock = 0;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU002")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When
    replaceWarehouseUseCase.replace(newWarehouse);

    // Then
    verify(warehouseStore).update(any(Warehouse.class));
  }

  /**
   * Tests warehouse replacement with exact capacity matching stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Replacement succeeds when new capacity exactly equals old stock
   *   <li>Boundary condition is handled correctly
   * </ul>
   */
  @Test
  @DisplayName("Should replace warehouse when new capacity exactly matches old stock")
  void testReplaceWarehouse_ExactCapacityMatch() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU003";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 750;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU003";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 750; // Exact match with old stock
    newWarehouse.stock = 750;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU003")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When
    replaceWarehouseUseCase.replace(newWarehouse);

    // Then
    verify(warehouseStore).update(any(Warehouse.class));
  }

  /**
   * Tests warehouse replacement when stock is null in old warehouse.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Null stock is treated as zero for validation purposes
   *   <li>New warehouse stock must also be zero or null
   * </ul>
   */
  @Test
  @DisplayName("Should replace warehouse when old stock is null")
  void testReplaceWarehouse_NullOldStock() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU004";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = null;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU004";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 800;
    newWarehouse.stock = 0; // Should match null (treated as 0)

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU004")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When
    replaceWarehouseUseCase.replace(newWarehouse);

    // Then
    verify(warehouseStore).update(any(Warehouse.class));
  }

  // ==================== Negative Test Cases ====================

  /**
   * Tests that replacement fails when warehouse does not exist.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Exception is thrown when attempting to replace non-existent warehouse
   *   <li>Appropriate error message is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when warehouse does not exist")
  void testReplaceWarehouse_WarehouseNotFound() {
    // Given
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "NONEXISTENT";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 1000;
    newWarehouse.stock = 0;

    when(warehouseStore.findByBusinessUnitCode("NONEXISTENT")).thenReturn(null);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("does not exist"));
    assertTrue(exception.getMessage().contains("NONEXISTENT"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that replacement fails when old warehouse is archived.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Cannot replace an archived warehouse
   *   <li>Appropriate error message is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when old warehouse is archived")
  void testReplaceWarehouse_ArchivedWarehouse() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU005";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 500;
    oldWarehouse.archivedAt = LocalDateTime.now(); // Already archived

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU005";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 1500;
    newWarehouse.stock = 500;

    when(warehouseStore.findByBusinessUnitCode("BU005")).thenReturn(oldWarehouse);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("Cannot replace an archived warehouse"));
    assertTrue(exception.getMessage().contains("BU005"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that replacement fails when new location is invalid.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Location must be valid before replacement
   *   <li>Appropriate error message is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when new location is invalid")
  void testReplaceWarehouse_InvalidLocation() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU006";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 500;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU006";
    newWarehouse.location = "INVALID_LOCATION";
    newWarehouse.capacity = 1500;
    newWarehouse.stock = 500;

    when(warehouseStore.findByBusinessUnitCode("BU006")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("INVALID_LOCATION")).thenReturn(null);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("is not a valid location"));
    assertTrue(exception.getMessage().contains("INVALID_LOCATION"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that replacement fails when new capacity is zero.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Capacity must be greater than zero
   *   <li>Appropriate error message is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when new capacity is zero")
  void testReplaceWarehouse_ZeroCapacity() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU007";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 0;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU007";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 0;
    newWarehouse.stock = 0;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU007")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("capacity must be greater than zero"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that replacement fails when new capacity is null.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Capacity cannot be null
   *   <li>Appropriate error message is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when new capacity is null")
  void testReplaceWarehouse_NullCapacity() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU008";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 0;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU008";
    newWarehouse.location = "LA";
    newWarehouse.capacity = null;
    newWarehouse.stock = 0;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU008")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("capacity must be greater than zero"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that replacement fails when new capacity cannot accommodate old stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>New warehouse capacity must be >= old warehouse stock
   *   <li>Appropriate error message with both values is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when new capacity cannot accommodate old stock")
  void testReplaceWarehouse_CapacityTooSmall() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU009";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 800;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU009";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 700; // Less than old stock of 800
    newWarehouse.stock = 800;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU009")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("cannot accommodate stock"));
    assertTrue(exception.getMessage().contains("700"));
    assertTrue(exception.getMessage().contains("800"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that replacement fails when new stock does not match old stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>New warehouse stock must exactly match old warehouse stock
   *   <li>Appropriate error message with both values is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when new stock does not match old stock")
  void testReplaceWarehouse_StockMismatch() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU010";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 500;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU010";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 1500;
    newWarehouse.stock = 600; // Does not match old stock of 500

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU010")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("must match the old warehouse stock"));
    assertTrue(exception.getMessage().contains("600"));
    assertTrue(exception.getMessage().contains("500"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that replacement fails when new stock is null but old stock is not.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Null new stock (treated as 0) must match old stock value
   *   <li>Stock matching validation handles null values correctly
   * </ul>
   */
  @Test
  @DisplayName("Should fail when new stock is null but old stock is not zero")
  void testReplaceWarehouse_NewStockNullMismatch() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU011";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 500;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU011";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 1500;
    newWarehouse.stock = null; // Treated as 0, doesn't match 500

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU011")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("must match the old warehouse stock"));
    verify(warehouseStore, never()).update(any());
  }

  // ==================== Error/Edge Case Tests ====================

  /**
   * Tests warehouse replacement with negative capacity.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Negative capacity is rejected
   *   <li>Validation catches invalid capacity values
   * </ul>
   */
  @Test
  @DisplayName("Should fail when new capacity is negative")
  void testReplaceWarehouse_NegativeCapacity() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU012";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 0;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU012";
    newWarehouse.location = "LA";
    newWarehouse.capacity = -100;
    newWarehouse.stock = 0;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU012")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> replaceWarehouseUseCase.replace(newWarehouse));

    assertTrue(exception.getMessage().contains("capacity must be greater than zero"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that business unit code is preserved during replacement.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Business unit code remains unchanged after replacement
   *   <li>This ensures cost history preservation (costs are linked to business unit code)
   * </ul>
   */
  @Test
  @DisplayName("Should preserve business unit code during replacement")
  void testReplaceWarehouse_PreservesBusinessUnitCode() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU_PRESERVE";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 300;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU_PRESERVE";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 1200;
    newWarehouse.stock = 300;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU_PRESERVE")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When
    replaceWarehouseUseCase.replace(newWarehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse updatedWarehouse = warehouseCaptor.getValue();
    assertEquals(
        "BU_PRESERVE",
        updatedWarehouse.businessUnitCode,
        "Business unit code should be preserved for cost history");
  }

  /**
   * Tests that archivedAt is set to null during replacement.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Replaced warehouse is marked as active (archivedAt = null)
   *   <li>This ensures the warehouse can be used for operations
   * </ul>
   */
  @Test
  @DisplayName("Should set archivedAt to null during replacement")
  void testReplaceWarehouse_SetsArchivedAtToNull() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.businessUnitCode = "BU013";
    oldWarehouse.location = "NYC";
    oldWarehouse.capacity = 1000;
    oldWarehouse.stock = 400;
    oldWarehouse.archivedAt = null;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "BU013";
    newWarehouse.location = "LA";
    newWarehouse.capacity = 1100;
    newWarehouse.stock = 400;

    Location location = new Location("LA", 5, 10000);

    when(warehouseStore.findByBusinessUnitCode("BU013")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("LA")).thenReturn(location);

    // When
    replaceWarehouseUseCase.replace(newWarehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse updatedWarehouse = warehouseCaptor.getValue();
    assertNull(updatedWarehouse.archivedAt, "ArchivedAt should be null for active warehouse");
  }
}