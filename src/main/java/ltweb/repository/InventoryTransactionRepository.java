package ltweb.repository;

import ltweb.entity.InventoryTransaction;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    List<InventoryTransaction> findByPackageItemId(Long packageId);

    List<InventoryTransaction> findByRelatedShipmentId(Long shipmentId);

    @Query("SELECT it FROM InventoryTransaction it WHERE it.warehouse.id = :warehouseId AND it.createdAt BETWEEN :startDate AND :endDate")
    List<InventoryTransaction> findByWarehouseAndDateRange(@Param("warehouseId") Long warehouseId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}