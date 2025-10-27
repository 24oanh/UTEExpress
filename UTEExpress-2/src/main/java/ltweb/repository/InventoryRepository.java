package ltweb.repository;

import ltweb.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    List<Inventory> findByWarehouseId(Long warehouseId);
    
    Optional<Inventory> findByWarehouseIdAndPackageItemId(Long warehouseId, Long packageId);
    
    List<Inventory> findByPackageItemId(Long packageId);
    
    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId AND i.remainingQuantity > 0")
    List<Inventory> findAvailableInventoryByWarehouseId(@Param("warehouseId") Long warehouseId);
    
    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.warehouse.id = :warehouseId")
    Integer getTotalQuantityByWarehouseId(@Param("warehouseId") Long warehouseId);
    
    @Query("SELECT SUM(i.remainingQuantity) FROM Inventory i WHERE i.warehouse.id = :warehouseId")
    Integer getTotalRemainingQuantityByWarehouseId(@Param("warehouseId") Long warehouseId);
}