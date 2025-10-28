package ltweb.repository;

import ltweb.entity.ShipmentLeg;
import ltweb.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentLegRepository extends JpaRepository<ShipmentLeg, Long> {
	List<ShipmentLeg> findByShipmentIdOrderByLegOrder(Long shipmentId);

	Optional<ShipmentLeg> findByShipmentIdAndLegOrder(Long shipmentId, Integer legOrder);

	List<ShipmentLeg> findByShipperIdAndStatus(Long shipperId, ShipmentStatus status);

	Optional<ShipmentLeg> findFirstByShipmentIdAndStatusOrderByLegOrder(Long shipmentId, ShipmentStatus status);

	List<ShipmentLeg> findByShipmentIdOrderByLegSequence(Long shipmentId);

	Optional<ShipmentLeg> findByShipmentIdAndLegSequence(Long shipmentId, Integer legSequence);

	Optional<ShipmentLeg> findFirstByShipmentIdAndStatusOrderByLegSequence(Long shipmentId, ShipmentStatus status);
}