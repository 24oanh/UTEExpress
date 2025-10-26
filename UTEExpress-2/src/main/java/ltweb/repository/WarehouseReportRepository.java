package ltweb.repository;

import ltweb.entity.ReportType;
import ltweb.entity.WarehouseReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WarehouseReportRepository extends JpaRepository<WarehouseReport, Long> {
    
    List<WarehouseReport> findByWarehouseId(Long warehouseId);
    
    List<WarehouseReport> findByWarehouseIdAndReportType(Long warehouseId, ReportType reportType);
    
    List<WarehouseReport> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
    
    @Query("SELECT wr FROM WarehouseReport wr WHERE wr.warehouse.id = :warehouseId AND wr.startDate >= :startDate AND wr.endDate <= :endDate")
    List<WarehouseReport> findByWarehouseIdAndDateRange(@Param("warehouseId") Long warehouseId, 
                                                         @Param("startDate") LocalDateTime startDate, 
                                                         @Param("endDate") LocalDateTime endDate);
}