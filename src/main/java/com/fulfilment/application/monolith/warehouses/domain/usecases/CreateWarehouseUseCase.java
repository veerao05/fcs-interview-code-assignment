package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  @Inject
  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // Validation 1: Business Unit Code Verification
    // Ensure that the specified business unit code for the warehouse doesn't already exist
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      throw new WarehouseValidationException(
          "Warehouse with business unit code " + warehouse.businessUnitCode + " already exists");
    }

    // Validation 2: Location Validation
    // Confirm that the warehouse location is valid
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location " + warehouse.location + " is not a valid location");
    }

    // Validation 3: Warehouse Creation Feasibility
    // Check if a new warehouse can be created at the specified location
    // or if the maximum number of warehouses has already been reached
    List<Warehouse> warehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location))
            .filter(w -> w.archivedAt == null) // Only count active warehouses
            .toList();

    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ") has been reached for location "
              + warehouse.location);
    }

    // Validation 4: Capacity and Stock Validation
    // Validate the warehouse capacity
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new WarehouseValidationException("Warehouse capacity must be greater than zero");
    }

    // Ensure capacity does not exceed the maximum capacity associated with the location
    int totalCapacityAtLocation =
        warehousesAtLocation.stream()
            .mapToInt(w -> w.capacity != null ? w.capacity : 0)
            .sum();

    if (totalCapacityAtLocation + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Total capacity at location "
              + warehouse.location
              + " would exceed maximum capacity of "
              + location.maxCapacity
              + ". Current total: "
              + totalCapacityAtLocation
              + ", adding: "
              + warehouse.capacity);
    }

    // Validate that capacity can handle the stock informed
    if (warehouse.stock != null && warehouse.stock > warehouse.capacity) {
      throw new WarehouseValidationException(
          "Warehouse stock (" + warehouse.stock + ") cannot exceed capacity (" + warehouse.capacity + ")");
    }

    // Set creation timestamp
    warehouse.createdAt = LocalDateTime.now();

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
