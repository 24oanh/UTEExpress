package ltweb.repository;

import ltweb.entity.Order;
import ltweb.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByWarehouseId(Long warehouseId);

    List<Order> findByShipperId(Long shipperId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByWarehouseIdAndStatus(Long warehouseId, OrderStatus status);

    List<Order> findByIsConfirmedFalseAndWarehouseId(Long warehouseId);

    List<Order> findByStatusAndWarehouseId(OrderStatus status, Long warehouseId);

    List<Order> findByShipperIdAndStatus(Long shipperId, OrderStatus status);
    // OrderRepository.java - Thêm methods

    List<Order> findByDestinationWarehouseId(Long destinationWarehouseId);

    List<Order> findByDestinationWarehouseIdAndStatus(Long destinationWarehouseId, OrderStatus status);

    long countByDestinationWarehouseIdAndStatus(Long destinationWarehouseId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.warehouse.id = :warehouseId AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByWarehouseIdAndDateRange(@Param("warehouseId") Long warehouseId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByWarehouseIdAndStatus(Long warehouseId, OrderStatus status);
}