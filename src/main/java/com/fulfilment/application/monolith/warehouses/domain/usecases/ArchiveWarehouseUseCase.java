package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  @Inject
  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    // Validation: Check if warehouse exists
    if (warehouse == null) {
      throw new WarehouseValidationException("Warehouse cannot be null");
    }

    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing == null) {
      throw new WarehouseValidationException(
          "Warehouse with business unit code " + warehouse.businessUnitCode + " does not exist");
    }

    // Validation: Check if warehouse is already archived
    if (existing.archivedAt != null) {
      throw new WarehouseValidationException(
          "Warehouse with business unit code " + warehouse.businessUnitCode + " is already archived");
    }

    // Set the archived timestamp
    existing.archivedAt = LocalDateTime.now();

    // Update the warehouse with archived status
    warehouseStore.update(existing);
  }
}
