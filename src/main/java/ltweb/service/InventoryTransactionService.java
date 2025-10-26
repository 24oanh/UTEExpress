package ltweb.service;

import lombok.RequiredArgsConstructor;
import ltweb.entity.Inventory;
import ltweb.entity.InventoryTransaction;
import ltweb.entity.LocationStatus;
import ltweb.entity.Shipment;
import ltweb.entity.Warehouse;
import ltweb.entity.WarehouseLocation;
import ltweb.repository.InventoryRepository;
import ltweb.repository.InventoryTransactionRepository;
import ltweb.repository.PackageRepository;
import ltweb.repository.WarehouseLocationRepository;
import ltweb.entity.Package;
import ltweb.entity.TransactionType;
import ltweb.entity.User;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryTransactionService {
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryRepository inventoryRepository;
    private final PackageRepository packageRepository;
    private final WarehouseLocationRepository locationRepository;

    @Transactional
    public InventoryTransaction recordInbound(Long warehouseId, Long packageId, Integer quantity,
            Long toLocationId, User performedBy, String notes) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));

        WarehouseLocation toLocation = locationRepository.findById(toLocationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        // Update inventory
        Inventory inventory = inventoryRepository.findByWarehouseIdAndPackageItemId(warehouseId, packageId)
                .orElse(Inventory.builder()
                        .warehouse(Warehouse.builder().id(warehouseId).build())
                        .packageItem(pkg)
                        .quantity(0)
                        .deliveredQuantity(0)
                        .remainingQuantity(0)
                        .build());

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory.setRemainingQuantity(inventory.getRemainingQuantity() + quantity);
        inventoryRepository.save(inventory);

        // Update package location
        pkg.setWarehouseLocationId(toLocationId);
        packageRepository.save(pkg);

        // Update location status
        toLocation.setStatus(LocationStatus.OCCUPIED);
        toLocation.setPackageItem(pkg);
        locationRepository.save(toLocation);

        // Record transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .warehouse(Warehouse.builder().id(warehouseId).build())
                .packageItem(pkg)
                .type(TransactionType.INBOUND)
                .quantity(quantity)
                .toLocation(toLocation)
                .notes(notes)
                .performedBy(performedBy)
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional
    public InventoryTransaction recordOutbound(Long warehouseId, Long packageId, Integer quantity,
            Long shipmentId, User performedBy, String notes) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));

        Inventory inventory = inventoryRepository.findByWarehouseIdAndPackageItemId(warehouseId, packageId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (inventory.getRemainingQuantity() < quantity) {
            throw new RuntimeException("Insufficient inventory");
        }

        // Update inventory
        inventory.setRemainingQuantity(inventory.getRemainingQuantity() - quantity);
        inventory.setDeliveredQuantity(inventory.getDeliveredQuantity() + quantity);
        inventoryRepository.save(inventory);

        // Clear location if empty
        if (pkg.getWarehouseLocationId() != null) {
            WarehouseLocation fromLocation = locationRepository.findById(pkg.getWarehouseLocationId())
                    .orElse(null);
            if (fromLocation != null && inventory.getRemainingQuantity() == 0) {
                fromLocation.setStatus(LocationStatus.EMPTY);
                fromLocation.setPackageItem(null);
                locationRepository.save(fromLocation);
                pkg.setWarehouseLocationId(null);
                packageRepository.save(pkg);
            }
        }

        // Record transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .warehouse(Warehouse.builder().id(warehouseId).build())
                .packageItem(pkg)
                .type(TransactionType.OUTBOUND)
                .quantity(quantity)
                .relatedShipment(shipmentId != null ? Shipment.builder().id(shipmentId).build() : null)
                .notes(notes)
                .performedBy(performedBy)
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional
    public InventoryTransaction transferLocation(Long packageId, Long fromLocationId,
            Long toLocationId, User performedBy, String notes) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));

        WarehouseLocation fromLocation = locationRepository.findById(fromLocationId)
                .orElseThrow(() -> new RuntimeException("From location not found"));
        WarehouseLocation toLocation = locationRepository.findById(toLocationId)
                .orElseThrow(() -> new RuntimeException("To location not found"));

        // Update locations
        fromLocation.setStatus(LocationStatus.EMPTY);
        fromLocation.setPackageItem(null);
        locationRepository.save(fromLocation);

        toLocation.setStatus(LocationStatus.OCCUPIED);
        toLocation.setPackageItem(pkg);
        locationRepository.save(toLocation);

        // Update package
        pkg.setWarehouseLocationId(toLocationId);
        packageRepository.save(pkg);

        // Record transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .warehouse(fromLocation.getWarehouse())
                .packageItem(pkg)
                .type(TransactionType.TRANSFER)
                .quantity(1)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .notes(notes)
                .performedBy(performedBy)
                .build();

        return transactionRepository.save(transaction);
    }

    public List<InventoryTransaction> getTransactionsByWarehouse(Long warehouseId) {
        return transactionRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
    }

    public List<InventoryTransaction> getTransactionsByPackage(Long packageId) {
        return transactionRepository.findByPackageItemId(packageId);
    }

    public List<InventoryTransaction> getTransactionsByShipment(Long shipmentId) {
        return transactionRepository.findByRelatedShipmentId(shipmentId);
    }
}