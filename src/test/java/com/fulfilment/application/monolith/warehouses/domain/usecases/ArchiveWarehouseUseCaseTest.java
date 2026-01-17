package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.junit.QuarkusTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Comprehensive test suite for {@link ArchiveWarehouseUseCase}.
 *
 * <p>This test suite validates the warehouse archiving functionality including:
 *
 * <ul>
 *   <li>Positive scenarios: Successful warehouse archiving with valid data
 *   <li>Negative scenarios: Validation failures for business rules
 *   <li>Error scenarios: Edge cases and boundary conditions
 * </ul>
 *
 * <p>The tests ensure proper enforcement of business rules:
 *
 * <ul>
 *   <li>Warehouse cannot be null
 *   <li>Warehouse must exist before archiving
 *   <li>Warehouse must not be already archived
 *   <li>archivedAt timestamp is set correctly
 * </ul>
 */
@QuarkusTest
@DisplayName("ArchiveWarehouseUseCase Tests")
class ArchiveWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @BeforeEach
  void setUp() {
    warehouseStore = Mockito.mock(WarehouseStore.class);
    archiveWarehouseUseCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  // ==================== Positive Test Cases ====================

  /**
   * Tests successful warehouse archiving with valid data.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Warehouse is successfully archived when all validations pass
   *   <li>archivedAt timestamp is set correctly
   *   <li>Update operation is called on the warehouse store
   * </ul>
   */
  @Test
  @DisplayName("Should successfully archive warehouse with valid data")
  void testArchiveWarehouse_Success() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU001";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = 500;
    warehouse.archivedAt = null;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU001";
    existingWarehouse.location = "NYC";
    existingWarehouse.capacity = 1000;
    existingWarehouse.stock = 500;
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU001")).thenReturn(existingWarehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse archivedWarehouse = warehouseCaptor.getValue();
    assertNotNull(archivedWarehouse.archivedAt);
    assertEquals("BU001", archivedWarehouse.businessUnitCode);
  }

  /**
   * Tests archiving warehouse with zero stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Warehouse with zero stock can be archived
   *   <li>archivedAt timestamp is set correctly
   * </ul>
   */
  @Test
  @DisplayName("Should archive warehouse with zero stock")
  void testArchiveWarehouse_ZeroStock() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU002";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = 0;
    warehouse.archivedAt = null;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU002";
    existingWarehouse.location = "NYC";
    existingWarehouse.capacity = 1000;
    existingWarehouse.stock = 0;
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU002")).thenReturn(existingWarehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse archivedWarehouse = warehouseCaptor.getValue();
    assertNotNull(archivedWarehouse.archivedAt);
  }

  /**
   * Tests archiving warehouse with full stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Warehouse with full stock (stock == capacity) can be archived
   *   <li>archivedAt timestamp is set correctly
   * </ul>
   */
  @Test
  @DisplayName("Should archive warehouse with full stock")
  void testArchiveWarehouse_FullStock() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU003";
    warehouse.location = "LA";
    warehouse.capacity = 1000;
    warehouse.stock = 1000;
    warehouse.archivedAt = null;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU003";
    existingWarehouse.location = "LA";
    existingWarehouse.capacity = 1000;
    existingWarehouse.stock = 1000;
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU003")).thenReturn(existingWarehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse archivedWarehouse = warehouseCaptor.getValue();
    assertNotNull(archivedWarehouse.archivedAt);
    assertEquals(1000, archivedWarehouse.stock);
  }

  /**
   * Tests that archivedAt timestamp is set to current time.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>archivedAt is set to a timestamp close to LocalDateTime.now()
   *   <li>Timestamp is not in the future
   *   <li>Timestamp is recent (within last few seconds)
   * </ul>
   */
  @Test
  @DisplayName("Should set archivedAt to current timestamp")
  void testArchiveWarehouse_SetsCurrentTimestamp() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU004";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = 500;
    warehouse.archivedAt = null;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU004";
    existingWarehouse.location = "NYC";
    existingWarehouse.capacity = 1000;
    existingWarehouse.stock = 500;
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU004")).thenReturn(existingWarehouse);

    LocalDateTime beforeArchive = LocalDateTime.now();

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    LocalDateTime afterArchive = LocalDateTime.now();

    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse archivedWarehouse = warehouseCaptor.getValue();
    assertNotNull(archivedWarehouse.archivedAt);
    assertTrue(
        archivedWarehouse.archivedAt.isAfter(beforeArchive.minusSeconds(1))
            || archivedWarehouse.archivedAt.isEqual(beforeArchive));
    assertTrue(
        archivedWarehouse.archivedAt.isBefore(afterArchive.plusSeconds(1))
            || archivedWarehouse.archivedAt.isEqual(afterArchive));
  }

  // ==================== Negative Test Cases ====================

  /**
   * Tests that archiving fails when warehouse is null.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Exception is thrown when attempting to archive null warehouse
   *   <li>Appropriate error message is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when warehouse is null")
  void testArchiveWarehouse_NullWarehouse() {
    // Given
    Warehouse warehouse = null;

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class, () -> archiveWarehouseUseCase.archive(warehouse));

    assertTrue(exception.getMessage().contains("Warehouse cannot be null"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that archiving fails when warehouse does not exist.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Exception is thrown when attempting to archive non-existent warehouse
   *   <li>Appropriate error message is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when warehouse does not exist")
  void testArchiveWarehouse_WarehouseNotFound() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NONEXISTENT";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = 500;

    when(warehouseStore.findByBusinessUnitCode("NONEXISTENT")).thenReturn(null);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class, () -> archiveWarehouseUseCase.archive(warehouse));

    assertTrue(exception.getMessage().contains("does not exist"));
    assertTrue(exception.getMessage().contains("NONEXISTENT"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that archiving fails when warehouse is already archived.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Exception is thrown when attempting to archive already-archived warehouse
   *   <li>Appropriate error message is provided
   *   <li>No update operation is performed
   * </ul>
   */
  @Test
  @DisplayName("Should fail when warehouse is already archived")
  void testArchiveWarehouse_AlreadyArchived() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU005";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = 500;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU005";
    existingWarehouse.location = "NYC";
    existingWarehouse.capacity = 1000;
    existingWarehouse.stock = 500;
    existingWarehouse.archivedAt = LocalDateTime.now().minusDays(1); // Already archived

    when(warehouseStore.findByBusinessUnitCode("BU005")).thenReturn(existingWarehouse);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class, () -> archiveWarehouseUseCase.archive(warehouse));

    assertTrue(exception.getMessage().contains("is already archived"));
    assertTrue(exception.getMessage().contains("BU005"));
    verify(warehouseStore, never()).update(any());
  }

  /**
   * Tests that archiving fails when warehouse was archived recently.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Cannot archive a warehouse that was just archived
   *   <li>archivedAt timestamp being non-null is the determinant factor
   * </ul>
   */
  @Test
  @DisplayName("Should fail when warehouse was archived recently")
  void testArchiveWarehouse_RecentlyArchived() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU006";
    warehouse.location = "LA";
    warehouse.capacity = 1000;
    warehouse.stock = 300;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU006";
    existingWarehouse.location = "LA";
    existingWarehouse.capacity = 1000;
    existingWarehouse.stock = 300;
    existingWarehouse.archivedAt = LocalDateTime.now(); // Just archived

    when(warehouseStore.findByBusinessUnitCode("BU006")).thenReturn(existingWarehouse);

    // When & Then
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class, () -> archiveWarehouseUseCase.archive(warehouse));

    assertTrue(exception.getMessage().contains("is already archived"));
    verify(warehouseStore, never()).update(any());
  }

  // ==================== Error/Edge Case Tests ====================

  /**
   * Tests archiving warehouse with null stock.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Warehouse with null stock can be archived
   *   <li>Null values are handled correctly
   * </ul>
   */
  @Test
  @DisplayName("Should archive warehouse with null stock")
  void testArchiveWarehouse_NullStock() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU007";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = null;
    warehouse.archivedAt = null;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU007";
    existingWarehouse.location = "NYC";
    existingWarehouse.capacity = 1000;
    existingWarehouse.stock = null;
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU007")).thenReturn(existingWarehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse archivedWarehouse = warehouseCaptor.getValue();
    assertNotNull(archivedWarehouse.archivedAt);
    assertNull(archivedWarehouse.stock);
  }

  /**
   * Tests that existing warehouse data is preserved except archivedAt.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Business unit code remains unchanged
   *   <li>Location remains unchanged
   *   <li>Capacity remains unchanged
   *   <li>Stock remains unchanged
   *   <li>Only archivedAt is modified
   * </ul>
   */
  @Test
  @DisplayName("Should preserve all warehouse data except archivedAt")
  void testArchiveWarehouse_PreservesWarehouseData() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU008";
    warehouse.location = "LA";
    warehouse.capacity = 1500;
    warehouse.stock = 750;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU008";
    existingWarehouse.location = "LA";
    existingWarehouse.capacity = 1500;
    existingWarehouse.stock = 750;
    existingWarehouse.createdAt = LocalDateTime.now().minusDays(30);
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU008")).thenReturn(existingWarehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse archivedWarehouse = warehouseCaptor.getValue();
    assertEquals("BU008", archivedWarehouse.businessUnitCode);
    assertEquals("LA", archivedWarehouse.location);
    assertEquals(1500, archivedWarehouse.capacity);
    assertEquals(750, archivedWarehouse.stock);
    assertNotNull(archivedWarehouse.archivedAt);
    assertNotNull(archivedWarehouse.createdAt);
  }

  /**
   * Tests warehouse archiving when business unit code has special characters.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Business unit codes with special characters are handled correctly
   *   <li>Archiving works regardless of business unit code format
   * </ul>
   */
  @Test
  @DisplayName("Should archive warehouse with special characters in business unit code")
  void testArchiveWarehouse_SpecialCharactersInBusinessUnitCode() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU-2024-001_NYC";
    warehouse.location = "NYC";
    warehouse.capacity = 2000;
    warehouse.stock = 1000;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU-2024-001_NYC";
    existingWarehouse.location = "NYC";
    existingWarehouse.capacity = 2000;
    existingWarehouse.stock = 1000;
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU-2024-001_NYC")).thenReturn(existingWarehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(warehouseCaptor.capture());

    Warehouse archivedWarehouse = warehouseCaptor.getValue();
    assertEquals("BU-2024-001_NYC", archivedWarehouse.businessUnitCode);
    assertNotNull(archivedWarehouse.archivedAt);
  }

  /**
   * Tests that update is called exactly once.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Update operation is called exactly once
   *   <li>No duplicate updates occur
   * </ul>
   */
  @Test
  @DisplayName("Should call update exactly once")
  void testArchiveWarehouse_CallsUpdateOnce() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU009";
    warehouse.location = "NYC";
    warehouse.capacity = 1000;
    warehouse.stock = 500;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU009";
    existingWarehouse.location = "NYC";
    existingWarehouse.capacity = 1000;
    existingWarehouse.stock = 500;
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU009")).thenReturn(existingWarehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    verify(warehouseStore, times(1)).update(any(Warehouse.class));
  }

  /**
   * Tests that findByBusinessUnitCode is called exactly once.
   *
   * <p>Verifies:
   *
   * <ul>
   *   <li>Lookup operation is called exactly once
   *   <li>No unnecessary lookups occur
   * </ul>
   */
  @Test
  @DisplayName("Should call findByBusinessUnitCode exactly once")
  void testArchiveWarehouse_CallsFindOnce() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU010";
    warehouse.location = "LA";
    warehouse.capacity = 1200;
    warehouse.stock = 600;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "BU010";
    existingWarehouse.location = "LA";
    existingWarehouse.capacity = 1200;
    existingWarehouse.stock = 600;
    existingWarehouse.archivedAt = null;

    when(warehouseStore.findByBusinessUnitCode("BU010")).thenReturn(existingWarehouse);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    verify(warehouseStore, times(1)).findByBusinessUnitCode("BU010");
  }
}
