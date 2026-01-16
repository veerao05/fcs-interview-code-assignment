package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CreateWarehouseUseCase.
 *
 * This test class verifies the warehouse creation logic including all business validations:
 * - Business Unit Code uniqueness verification
 * - Location validation
 * - Warehouse creation feasibility at location
 * - Capacity and stock validation
 *
 * Test Coverage:
 * - Positive scenarios: Successful warehouse creation with valid data
 * - Negative scenarios: Invalid inputs, duplicate codes, constraint violations
 * - Error scenarios: Location limits, capacity limits, validation failures
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Create Warehouse Use Case Unit Tests")
public class CreateWarehouseUseCaseTest {

    @Mock
    private WarehouseStore warehouseStore;

    @Mock
    private LocationResolver locationResolver;

    private CreateWarehouseUseCase useCase;

    @BeforeEach
    public void setup() {
        useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
    }

    // ==================== POSITIVE TEST CASES ====================

    /**
     * Positive Test: Create warehouse with valid data.
     *
     * Verifies:
     * - Warehouse is created successfully
     * - All validations pass
     * - CreatedAt timestamp is set
     * - Warehouse is persisted to store
     */
    @Test
    @DisplayName("Create warehouse with valid data should succeed")
    public void testCreateWarehouse_ValidData_ShouldSucceed() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-001";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 30;
        warehouse.stock = 10;

        Location location = new Location("ZWOLLE-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        // when
        useCase.create(warehouse);

        // then
        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseStore).create(captor.capture());

        Warehouse created = captor.getValue();
        assertEquals("WH-001", created.businessUnitCode);
        assertEquals("ZWOLLE-001", created.location);
        assertEquals(30, created.capacity);
        assertEquals(10, created.stock);
        assertNotNull(created.createdAt);
    }

    /**
     * Positive Test: Create warehouse with zero stock.
     *
     * Verifies:
     * - Warehouse can be created with zero stock
     * - Edge case of empty warehouse is handled
     */
    @Test
    @DisplayName("Create warehouse with zero stock should succeed")
    public void testCreateWarehouse_ZeroStock_ShouldSucceed() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-002";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 50;
        warehouse.stock = 0;

        Location location = new Location("AMSTERDAM-001", 5, 100);

        when(warehouseStore.findByBusinessUnitCode("WH-002")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        // when
        useCase.create(warehouse);

        // then
        verify(warehouseStore).create(any(Warehouse.class));
    }

    /**
     * Positive Test: Create warehouse at location with remaining capacity.
     *
     * Verifies:
     * - Warehouse can be created when location has available capacity
     * - Total capacity calculation includes existing warehouses
     */
    @Test
    @DisplayName("Create warehouse at location with remaining capacity should succeed")
    public void testCreateWarehouse_WithRemainingLocationCapacity_ShouldSucceed() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-003";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 40;
        warehouse.stock = 20;

        Location location = new Location("AMSTERDAM-001", 5, 100);

        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "WH-EXISTING";
        existing.location = "AMSTERDAM-001";
        existing.capacity = 50;
        existing.archivedAt = null;

        when(warehouseStore.findByBusinessUnitCode("WH-003")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(List.of(existing));

        // when
        useCase.create(warehouse);

        // then
        verify(warehouseStore).create(any(Warehouse.class));
    }

    /**
     * Positive Test: Create warehouse when archived warehouses exist.
     *
     * Verifies:
     * - Archived warehouses don't count toward location limits
     * - Only active warehouses are considered in validation
     */
    @Test
    @DisplayName("Create warehouse should ignore archived warehouses at location")
    public void testCreateWarehouse_WithArchivedWarehousesAtLocation_ShouldSucceed() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-004";
        warehouse.location = "TILBURG-001";
        warehouse.capacity = 30;
        warehouse.stock = 15;

        Location location = new Location("TILBURG-001", 1, 40);

        Warehouse archived = new Warehouse();
        archived.businessUnitCode = "WH-ARCHIVED";
        archived.location = "TILBURG-001";
        archived.capacity = 40;
        archived.archivedAt = java.time.LocalDateTime.now();

        when(warehouseStore.findByBusinessUnitCode("WH-004")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("TILBURG-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(List.of(archived));

        // when
        useCase.create(warehouse);

        // then
        verify(warehouseStore).create(any(Warehouse.class));
    }

    // ==================== NEGATIVE TEST CASES ====================

    /**
     * Negative Test: Create warehouse with duplicate business unit code.
     *
     * Verifies:
     * - Warehouse creation is rejected when business unit code already exists
     * - Appropriate validation error is thrown
     * - No warehouse is persisted
     */
    @Test
    @DisplayName("Create warehouse with duplicate business unit code should fail")
    public void testCreateWarehouse_DuplicateBusinessUnitCode_ShouldFail() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-001";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 30;
        warehouse.stock = 10;

        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "WH-001";

        when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);

        // when/then
        WarehouseValidationException exception = assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse)
        );

        assertEquals(
            "Warehouse with business unit code WH-001 already exists",
            exception.getMessage()
        );
        verify(warehouseStore, never()).create(any());
    }

    /**
     * Negative Test: Create warehouse with invalid location.
     *
     * Verifies:
     * - Warehouse creation is rejected for non-existent location
     * - Location validation occurs before other checks
     * - No warehouse is persisted
     */
    @Test
    @DisplayName("Create warehouse with invalid location should fail")
    public void testCreateWarehouse_InvalidLocation_ShouldFail() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-005";
        warehouse.location = "INVALID-LOCATION";
        warehouse.capacity = 30;
        warehouse.stock = 10;

        when(warehouseStore.findByBusinessUnitCode("WH-005")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("INVALID-LOCATION")).thenReturn(null);

        // when/then
        WarehouseValidationException exception = assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse)
        );

        assertEquals(
            "Location INVALID-LOCATION is not a valid location",
            exception.getMessage()
        );
        verify(warehouseStore, never()).create(any());
    }

    /**
     * Negative Test: Create warehouse when max number reached at location.
     *
     * Verifies:
     * - Warehouse creation is rejected when location is at capacity
     * - Maximum warehouse count per location is enforced
     * - Appropriate error message is returned
     */
    @Test
    @DisplayName("Create warehouse when max warehouses reached at location should fail")
    public void testCreateWarehouse_MaxWarehousesReached_ShouldFail() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-006";
        warehouse.location = "TILBURG-001";
        warehouse.capacity = 30;
        warehouse.stock = 10;

        Location location = new Location("TILBURG-001", 1, 40);

        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "WH-EXISTING";
        existing.location = "TILBURG-001";
        existing.capacity = 40;
        existing.archivedAt = null;

        when(warehouseStore.findByBusinessUnitCode("WH-006")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("TILBURG-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(List.of(existing));

        // when/then
        WarehouseValidationException exception = assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse)
        );

        assertEquals(
            "Maximum number of warehouses (1) has been reached for location TILBURG-001",
            exception.getMessage()
        );
        verify(warehouseStore, never()).create(any());
    }

    /**
     * Negative Test: Create warehouse with zero or negative capacity.
     *
     * Verifies:
     * - Warehouse capacity must be greater than zero
     * - Invalid capacity values are rejected
     */
    @Test
    @DisplayName("Create warehouse with zero capacity should fail")
    public void testCreateWarehouse_ZeroCapacity_ShouldFail() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-007";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 0;
        warehouse.stock = 0;

        Location location = new Location("ZWOLLE-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("WH-007")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        // when/then
        WarehouseValidationException exception = assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse)
        );

        assertEquals("Warehouse capacity must be greater than zero", exception.getMessage());
        verify(warehouseStore, never()).create(any());
    }

    /**
     * Negative Test: Create warehouse with null capacity.
     *
     * Verifies:
     * - Null capacity is treated as invalid
     * - Appropriate validation error is thrown
     */
    @Test
    @DisplayName("Create warehouse with null capacity should fail")
    public void testCreateWarehouse_NullCapacity_ShouldFail() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-008";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = null;
        warehouse.stock = 0;

        Location location = new Location("ZWOLLE-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("WH-008")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        // when/then
        WarehouseValidationException exception = assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse)
        );

        assertEquals("Warehouse capacity must be greater than zero", exception.getMessage());
        verify(warehouseStore, never()).create(any());
    }

    /**
     * Negative Test: Create warehouse exceeding location max capacity.
     *
     * Verifies:
     * - Total capacity at location cannot exceed max capacity
     * - Calculation includes existing warehouse capacities
     * - Detailed error message shows current and new capacity
     */
    @Test
    @DisplayName("Create warehouse exceeding location max capacity should fail")
    public void testCreateWarehouse_ExceedingLocationMaxCapacity_ShouldFail() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-009";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 60;
        warehouse.stock = 30;

        Location location = new Location("AMSTERDAM-001", 5, 100);

        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "WH-EXISTING";
        existing.location = "AMSTERDAM-001";
        existing.capacity = 50;
        existing.archivedAt = null;

        when(warehouseStore.findByBusinessUnitCode("WH-009")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(List.of(existing));

        // when/then
        WarehouseValidationException exception = assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse)
        );

        assertEquals(
            "Total capacity at location AMSTERDAM-001 would exceed maximum capacity of 100. Current total: 50, adding: 60",
            exception.getMessage()
        );
        verify(warehouseStore, never()).create(any());
    }

    /**
     * Negative Test: Create warehouse with stock exceeding capacity.
     *
     * Verifies:
     * - Stock cannot exceed warehouse capacity
     * - Validation catches impossible stock/capacity combinations
     */
    @Test
    @DisplayName("Create warehouse with stock exceeding capacity should fail")
    public void testCreateWarehouse_StockExceedsCapacity_ShouldFail() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-010";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 30;
        warehouse.stock = 50;

        Location location = new Location("ZWOLLE-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("WH-010")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        // when/then
        WarehouseValidationException exception = assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse)
        );

        assertEquals(
            "Warehouse stock (50) cannot exceed capacity (30)",
            exception.getMessage()
        );
        verify(warehouseStore, never()).create(any());
    }

    // ==================== ERROR CONDITION TESTS ====================

    /**
     * Error Test: Create warehouse at location with multiple existing warehouses.
     *
     * Verifies:
     * - System correctly calculates total capacity with multiple warehouses
     * - All active warehouses are included in capacity calculation
     */
    @Test
    @DisplayName("Create warehouse should correctly calculate capacity with multiple warehouses")
    public void testCreateWarehouse_MultipleWarehousesAtLocation_CorrectCapacityCalculation() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-011";
        warehouse.location = "AMSTERDAM-001";
        warehouse.capacity = 20;
        warehouse.stock = 10;

        Location location = new Location("AMSTERDAM-001", 5, 100);

        Warehouse existing1 = new Warehouse();
        existing1.businessUnitCode = "WH-E1";
        existing1.location = "AMSTERDAM-001";
        existing1.capacity = 40;
        existing1.archivedAt = null;

        Warehouse existing2 = new Warehouse();
        existing2.businessUnitCode = "WH-E2";
        existing2.location = "AMSTERDAM-001";
        existing2.capacity = 30;
        existing2.archivedAt = null;

        when(warehouseStore.findByBusinessUnitCode("WH-011")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(List.of(existing1, existing2));

        // when
        useCase.create(warehouse);

        // then - total capacity would be 40 + 30 + 20 = 90, which is under 100 limit
        verify(warehouseStore).create(any(Warehouse.class));
    }

    /**
     * Error Test: Create warehouse with capacity at exact location limit.
     *
     * Verifies:
     * - Warehouse can be created when total equals max capacity
     * - Boundary condition is handled correctly
     */
    @Test
    @DisplayName("Create warehouse with capacity exactly at location limit should succeed")
    public void testCreateWarehouse_ExactlyAtLocationLimit_ShouldSucceed() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-012";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 40;
        warehouse.stock = 20;

        Location location = new Location("ZWOLLE-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("WH-012")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        // when
        useCase.create(warehouse);

        // then
        verify(warehouseStore).create(any(Warehouse.class));
    }

    /**
     * Error Test: Create warehouse with stock equal to capacity.
     *
     * Verifies:
     * - Warehouse at full capacity is valid
     * - Boundary condition for stock/capacity is handled
     */
    @Test
    @DisplayName("Create warehouse with stock equal to capacity should succeed")
    public void testCreateWarehouse_StockEqualsCapacity_ShouldSucceed() {
        // given
        Warehouse warehouse = new Warehouse();
        warehouse.businessUnitCode = "WH-013";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 30;
        warehouse.stock = 30;

        Location location = new Location("ZWOLLE-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("WH-013")).thenReturn(null);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        when(warehouseStore.getAll()).thenReturn(new ArrayList<>());

        // when
        useCase.create(warehouse);

        // then
        verify(warehouseStore).create(any(Warehouse.class));
    }
}