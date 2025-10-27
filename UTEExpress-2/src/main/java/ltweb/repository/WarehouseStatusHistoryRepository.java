package ltweb.repository;

import ltweb.entity.WarehouseStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseStatusHistoryRepository extends JpaRepository<WarehouseStatusHistory, Long> {
    
    List<WarehouseStatusHistory> findByWarehouseId(Long warehouseId);
    
    List<WarehouseStatusHistory> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
}