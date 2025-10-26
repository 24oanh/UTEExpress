package ltweb.repository;

import ltweb.entity.InboundReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InboundReceiptRepository extends JpaRepository<InboundReceipt, Long> {
    
    List<InboundReceipt> findByWarehouseId(Long warehouseId);
    
    @Query("SELECT ir FROM InboundReceipt ir WHERE ir.warehouse.id = :warehouseId AND ir.receivedDate BETWEEN :startDate AND :endDate")
    List<InboundReceipt> findByWarehouseIdAndDateRange(@Param("warehouseId") Long warehouseId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT ir FROM InboundReceipt ir WHERE ir.warehouse.id = :warehouseId AND ir.receivedDate > :date")
    List<InboundReceipt> findByWarehouseIdAndReceivedDateAfter(@Param("warehouseId") Long warehouseId,
                                                               @Param("date") LocalDateTime date);
}