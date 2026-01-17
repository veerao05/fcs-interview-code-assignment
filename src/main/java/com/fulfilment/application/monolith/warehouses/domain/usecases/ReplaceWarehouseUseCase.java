package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  @Inject
  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // Find the existing warehouse to be replaced
    Warehouse oldWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);

    if (oldWarehouse == null) {
      throw new WarehouseValidationException(
          "Warehouse with business unit code " + newWarehouse.businessUnitCode + " does not exist");
    }

    // Validate that old warehouse is not already archived
    if (oldWarehouse.archivedAt != null) {
      throw new WarehouseValidationException(
          "Cannot replace an archived warehouse. Business unit code: " + newWarehouse.businessUnitCode);
    }

    // Validation: Location Validation
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location " + newWarehouse.location + " is not a valid location");
    }

    // Validation: Capacity must be greater than zero
    if (newWarehouse.capacity == null || newWarehouse.capacity <= 0) {
      throw new WarehouseValidationException("Warehouse capacity must be greater than zero");
    }

    // Validation: Capacity Accommodation
    // Ensure the new warehouse's capacity can accommodate the stock from the warehouse being replaced
    Integer oldStock = oldWarehouse.stock != null ? oldWarehouse.stock : 0;

    if (newWarehouse.capacity < oldStock) {
      throw new WarehouseValidationException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate stock from old warehouse ("
              + oldStock
              + ")");
    }

    // Validation: Stock Matching
    // Confirm that the stock of the new warehouse matches the stock of the previous warehouse
    Integer newStock = newWarehouse.stock != null ? newWarehouse.stock : 0;

    if (!newStock.equals(oldStock)) {
      throw new WarehouseValidationException(
          "New warehouse stock ("
              + newStock
              + ") must match the old warehouse stock ("
              + oldStock
              + ")");
    }

    // Update the existing warehouse record with new details
    // The business unit code remains the same, which allows cost history to be preserved
    // (cost records reference the business unit code with timestamps)
    oldWarehouse.location = newWarehouse.location;
    oldWarehouse.capacity = newWarehouse.capacity;
    oldWarehouse.stock = newWarehouse.stock;
    oldWarehouse.createdAt = LocalDateTime.now(); // Mark when the replacement occurred
    oldWarehouse.archivedAt = null; // Ensure the warehouse is active

    warehouseStore.update(oldWarehouse);
  }
}
