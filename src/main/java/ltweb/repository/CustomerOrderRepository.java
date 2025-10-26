package ltweb.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ltweb.entity.CustomerOrder;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByIsProcessedFalseOrderByCreatedAtDesc();

    List<CustomerOrder> findByFromWarehouseCodeAndIsProcessedFalseOrderByCreatedAtDesc(String warehouseCode);

    Optional<CustomerOrder> findByProcessedOrderId(Long orderId);

    @Modifying
    @Query("UPDATE CustomerOrder c SET c.isProcessed = true, c.processedOrderId = :orderId WHERE c.id = :id")
    void markAsProcessed(@Param("id") Long id, @Param("orderId") Long orderId);
}