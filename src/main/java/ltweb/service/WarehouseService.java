package ltweb.service;

import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final InboundReceiptDetailRepository inboundReceiptDetailRepository;
    private final OutboundReceiptRepository outboundReceiptRepository;
    private final OutboundReceiptDetailRepository outboundReceiptDetailRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseStatusHistoryRepository warehouseStatusHistoryRepository;
    private final PackageRepository packageRepository;

    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    public Warehouse getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + id));
    }

    public Warehouse getWarehouseByCode(String code) {
        return warehouseRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with code: " + code));
    }

    public Warehouse getWarehouseByUserId(Long userId) {
        return warehouseRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found for user id: " + userId));
    }

    @Transactional
    public Warehouse createWarehouse(Warehouse warehouse) {
        if (warehouseRepository.existsByCode(warehouse.getCode())) {
            throw new RuntimeException("Warehouse code already exists");
        }
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public Warehouse updateWarehouse(Long id, Warehouse warehouseDetails) {
        Warehouse warehouse = getWarehouseById(id);
        warehouse.setName(warehouseDetails.getName());
        warehouse.setAddress(warehouseDetails.getAddress());
        warehouse.setPhone(warehouseDetails.getPhone());
        warehouse.setEmail(warehouseDetails.getEmail());
        warehouse.setManager(warehouseDetails.getManager());
        warehouse.setTotalCapacity(warehouseDetails.getTotalCapacity());
        return warehouseRepository.save(warehouse);
    }

    public List<Inventory> getInventoryByWarehouseId(Long warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId);
    }

    public List<Inventory> getAvailableInventory(Long warehouseId) {
        return inventoryRepository.findAvailableInventoryByWarehouseId(warehouseId);
    }

    @Transactional
    public InboundReceipt createInboundReceipt(InboundReceipt inboundReceipt, List<InboundReceiptDetail> details) {
        InboundReceipt savedReceipt = inboundReceiptRepository.save(inboundReceipt);
        
        for (InboundReceiptDetail detail : details) {
            detail.setInboundReceipt(savedReceipt);
            inboundReceiptDetailRepository.save(detail);
            
            updateInventoryOnInbound(savedReceipt.getWarehouse().getId(), 
                    detail.getPackageItem().getId(), 
                    detail.getQuantity());
            
            if (detail.getWarehouseLocation() != null) {
                WarehouseLocation location = detail.getWarehouseLocation();
                location.setStatus(LocationStatus.OCCUPIED);
                location.setPackageItem(detail.getPackageItem());
                warehouseLocationRepository.save(location);
            }
            
            logWarehouseStatusHistory(savedReceipt.getWarehouse(), 
                    detail.getPackageItem(), 
                    ChangeType.INBOUND, 
                    null, 
                    "RECEIVED", 
                    detail.getQuantity(), 
                    savedReceipt.getReceivedBy(), 
                    "Inbound receipt: " + savedReceipt.getReceiptCode());
        }
        
        updateWarehouseCurrentStock(savedReceipt.getWarehouse().getId());
        return savedReceipt;
    }

    @Transactional
    public OutboundReceipt createOutboundReceipt(OutboundReceipt outboundReceipt, List<OutboundReceiptDetail> details) {
        OutboundReceipt savedReceipt = outboundReceiptRepository.save(outboundReceipt);
        
        for (OutboundReceiptDetail detail : details) {
            detail.setOutboundReceipt(savedReceipt);
            outboundReceiptDetailRepository.save(detail);
            
            updateInventoryOnOutbound(savedReceipt.getWarehouse().getId(), 
                    detail.getPackageItem().getId(), 
                    detail.getQuantity());
            
            if (detail.getWarehouseLocation() != null) {
                WarehouseLocation location = detail.getWarehouseLocation();
                location.setStatus(LocationStatus.EMPTY);
                location.setPackageItem(null);
                warehouseLocationRepository.save(location);
            }
            
            Package packageItem = detail.getPackageItem();
            packageItem.setStatus(PackageStatus.DANG_VAN_CHUYEN);
            packageRepository.save(packageItem);
            
            logWarehouseStatusHistory(savedReceipt.getWarehouse(), 
                    detail.getPackageItem(), 
                    ChangeType.OUTBOUND, 
                    "IN_WAREHOUSE", 
                    "OUT_FOR_DELIVERY", 
                    detail.getQuantity(), 
                    savedReceipt.getIssuedBy(), 
                    "Outbound receipt: " + savedReceipt.getReceiptCode());
        }
        
        updateWarehouseCurrentStock(savedReceipt.getWarehouse().getId());
        return savedReceipt;
    }

    @Transactional
    private void updateInventoryOnInbound(Long warehouseId, Long packageId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByWarehouseIdAndPackageItemId(warehouseId, packageId)
                .orElse(Inventory.builder()
                        .warehouse(warehouseRepository.findById(warehouseId).orElseThrow())
                        .packageItem(packageRepository.findById(packageId).orElseThrow())
                        .quantity(0)
                        .deliveredQuantity(0)
                        .remainingQuantity(0)
                        .build());
        
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory.setRemainingQuantity(inventory.getRemainingQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    @Transactional
    private void updateInventoryOnOutbound(Long warehouseId, Long packageId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByWarehouseIdAndPackageItemId(warehouseId, packageId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        
        if (inventory.getRemainingQuantity() < quantity) {
            throw new RuntimeException("Insufficient inventory");
        }
        
        inventory.setRemainingQuantity(inventory.getRemainingQuantity() - quantity);
        inventory.setDeliveredQuantity(inventory.getDeliveredQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    @Transactional
    private void updateWarehouseCurrentStock(Long warehouseId) {
        Integer totalStock = inventoryRepository.getTotalRemainingQuantityByWarehouseId(warehouseId);
        Warehouse warehouse = getWarehouseById(warehouseId);
        warehouse.setCurrentStock(totalStock != null ? totalStock : 0);
        warehouseRepository.save(warehouse);
    }

    @Transactional
    private void logWarehouseStatusHistory(Warehouse warehouse, Package packageItem, 
                                          ChangeType changeType, String oldStatus, 
                                          String newStatus, Integer quantity, 
                                          User user, String notes) {
        WarehouseStatusHistory history = WarehouseStatusHistory.builder()
                .warehouse(warehouse)
                .packageItem(packageItem)
                .changeType(changeType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .quantityChanged(quantity)
                .performedBy(user)
                .notes(notes)
                .build();
        warehouseStatusHistoryRepository.save(history);
    }

    public List<WarehouseLocation> getLocationsByWarehouseId(Long warehouseId) {
        return warehouseLocationRepository.findByWarehouseId(warehouseId);
    }

    public List<WarehouseLocation> getEmptyLocations(Long warehouseId) {
        return warehouseLocationRepository.findByWarehouseIdAndStatus(warehouseId, LocationStatus.EMPTY);
    }

    @Transactional
    public WarehouseLocation createLocation(WarehouseLocation location) {
        if (warehouseLocationRepository.existsByLocationCode(location.getLocationCode())) {
            throw new RuntimeException("Location code already exists");
        }
        return warehouseLocationRepository.save(location);
    }

    @Transactional
    public WarehouseLocation updateLocationStatus(Long locationId, LocationStatus status) {
        WarehouseLocation location = warehouseLocationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));
        location.setStatus(status);
        return warehouseLocationRepository.save(location);
    }

    public List<InboundReceipt> getInboundReceiptsByWarehouseId(Long warehouseId) {
        return inboundReceiptRepository.findByWarehouseId(warehouseId);
    }

    public List<OutboundReceipt> getOutboundReceiptsByWarehouseId(Long warehouseId) {
        return outboundReceiptRepository.findByWarehouseId(warehouseId);
    }

    @Transactional
    public InboundReceipt approveInboundReceipt(Long receiptId) {
        InboundReceipt receipt = inboundReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Receipt not found"));
        receipt.setStatus(ReceiptStatus.APPROVED);
        return inboundReceiptRepository.save(receipt);
    }

    @Transactional
    public OutboundReceipt approveOutboundReceipt(Long receiptId) {
        OutboundReceipt receipt = outboundReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Receipt not found"));
        receipt.setStatus(ReceiptStatus.APPROVED);
        return outboundReceiptRepository.save(receipt);
    }
}