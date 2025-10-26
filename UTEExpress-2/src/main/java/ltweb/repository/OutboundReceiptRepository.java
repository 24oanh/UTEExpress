package ltweb.repository;

import ltweb.entity.OutboundReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboundReceiptRepository extends JpaRepository<OutboundReceipt, Long> {
    
    List<OutboundReceipt> findByWarehouseId(Long warehouseId);
    
    @Query("SELECT or FROM OutboundReceipt or WHERE or.warehouse.id = :warehouseId AND or.issuedDate BETWEEN :startDate AND :endDate")
    List<OutboundReceipt> findByWarehouseIdAndDateRange(@Param("warehouseId") Long warehouseId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT or FROM OutboundReceipt or WHERE or.warehouse.id = :warehouseId AND or.issuedDate > :date")
    List<OutboundReceipt> findByWarehouseIdAndIssuedDateAfter(@Param("warehouseId") Long warehouseId,
                                                              @Param("date") LocalDateTime date);
}