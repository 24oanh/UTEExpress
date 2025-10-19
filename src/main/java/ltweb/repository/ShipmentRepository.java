package ltweb.repository;

import ltweb.entity.Shipment;
import ltweb.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    
    Optional<Shipment> findByShipmentCode(String shipmentCode);
    
    Optional<Shipment> findByOrderId(Long orderId);
    
    List<Shipment> findByShipperId(Long shipperId);
    
    List<Shipment> findByStatus(ShipmentStatus status);
    
    List<Shipment> findByShipperIdAndStatus(Long shipperId, ShipmentStatus status);
}