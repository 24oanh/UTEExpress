package ltweb.service;

import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentService {

	private final ShipmentRepository shipmentRepository;
	private final ShipperRepository shipperRepository;
	private final OrderRepository orderRepository;
	private final NotificationService notificationService;
	private final TrackingService trackingService;
	private final ShipmentLegService shipmentLegService;
	private final ShipmentLegRepository shipmentLegRepository;

	public List<Shipment> getAllShipments() {
		return shipmentRepository.findAll();
	}

	public Shipment getShipmentById(Long id) {
		return shipmentRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Shipment not found with id: " + id));
	}

	public Shipment getShipmentByCode(String shipmentCode) {
		return shipmentRepository.findByShipmentCode(shipmentCode)
				.orElseThrow(() -> new RuntimeException("Shipment not found with code: " + shipmentCode));
	}

	public Shipment getShipmentByOrderId(Long orderId) {
		return shipmentRepository.findByOrderId(orderId).orElse(null);
	}

	public List<Shipment> getShipmentsByShipperId(Long shipperId) {
		return shipmentRepository.findByShipperId(shipperId);
	}

	public List<Shipment> getShipmentsByStatus(ShipmentStatus status) {
		return shipmentRepository.findByStatus(status);
	}

	public List<Shipment> getShipmentsByShipperAndStatus(Long shipperId, ShipmentStatus status) {
		return shipmentRepository.findByShipperIdAndStatus(shipperId, status);
	}

	@Transactional
	public Shipment createShipment(Shipment shipment) {
		Shipment savedShipment = shipmentRepository.save(shipment);

		trackingService.createTracking(savedShipment, 0.0, 0.0, "Shipment created", TrackingStatus.STARTED);

		if (shipment.getOrder() != null && shipment.getOrder().getWarehouse() != null) {
			notificationService.createNotification("WAREHOUSE", shipment.getOrder().getWarehouse().getId(),
					"New shipment created: " + shipment.getShipmentCode(), NotificationType.NEW_DELIVERY,
					shipment.getOrder());
		}

		return savedShipment;
	}

	@Transactional
	public Shipment updateShipment(Long id, Shipment shipmentDetails) {
		Shipment shipment = getShipmentById(id);
		shipment.setNotes(shipmentDetails.getNotes());
		return shipmentRepository.save(shipment);
	}

	@Transactional
	public Shipment updateShipmentStatus(Long id, ShipmentStatus status) {
		Shipment shipment = getShipmentById(id);
		ShipmentStatus oldStatus = shipment.getStatus();
		shipment.setStatus(status);

		if (status == ShipmentStatus.IN_TRANSIT && oldStatus == ShipmentStatus.PENDING) {
			shipment.setPickupTime(LocalDateTime.now());

			Order order = shipment.getOrder();
			order.setStatus(OrderStatus.DANG_GIAO);
			orderRepository.save(order);

			ShipmentLeg firstLeg = shipmentLegRepository
					.findFirstByShipmentIdAndStatusOrderByLegSequence(id, ShipmentStatus.PENDING)
					.orElse(null);
			if (firstLeg != null) {
				firstLeg.setStatus(ShipmentStatus.IN_TRANSIT);
				firstLeg.setPickupTime(LocalDateTime.now());
				shipmentLegRepository.save(firstLeg);
			}

			trackingService.createTracking(shipment, 0.0, 0.0, "Shipment picked up and in transit",
					TrackingStatus.IN_PROGRESS);

			if (order.getWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE",
						order.getWarehouse().getId(),
						"Shipment in transit: " + shipment.getShipmentCode(),
						NotificationType.ORDER_ASSIGNED,
						order);
			}

			if (order.getDestinationWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE",
						order.getDestinationWarehouse().getId(),
						"Shipment in transit: " + shipment.getShipmentCode(),
						NotificationType.ORDER_ASSIGNED,
						order);
			}
		} else if (status == ShipmentStatus.DELIVERED) {
			shipment.setDeliveryTime(LocalDateTime.now());

			shipmentLegService.completeCurrentLegAndStartNext(id);
			updateShipperStatistics(shipment.getShipper().getId(), true);

			if (shipment.getOrder().getWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE", shipment.getOrder().getWarehouse().getId(),
						"Shipment delivered: " + shipment.getShipmentCode(), NotificationType.DELIVERY_COMPLETED,
						shipment.getOrder());
			}

			if (shipment.getOrder().getDestinationWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE",
						shipment.getOrder().getDestinationWarehouse().getId(),
						"Shipment delivered: " + shipment.getShipmentCode(),
						NotificationType.DELIVERY_COMPLETED,
						shipment.getOrder());
			}

		} else if (status == ShipmentStatus.FAILED) {
			shipmentLegService.failCurrentLeg(id, shipment.getNotes());
			updateShipperStatistics(shipment.getShipper().getId(), false);

			if (shipment.getOrder().getWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE", shipment.getOrder().getWarehouse().getId(),
						"Shipment failed: " + shipment.getShipmentCode(), NotificationType.ORDER_FAILED,
						shipment.getOrder());
			}
		}

		return shipmentRepository.save(shipment);
	}

	@Transactional
	public Shipment uploadProofImage(Long id, String imageUrl) {
		Shipment shipment = getShipmentById(id);
		shipment.setProofImageUrl(imageUrl);
		return shipmentRepository.save(shipment);
	}

	@Transactional
	private void updateShipperStatistics(Long shipperId, boolean isSuccessful) {
		Shipper shipper = shipperRepository.findById(shipperId)
				.orElseThrow(() -> new RuntimeException("Shipper not found"));

		shipper.setTotalDeliveries(shipper.getTotalDeliveries() + 1);
		if (isSuccessful) {
			shipper.setSuccessfulDeliveries(shipper.getSuccessfulDeliveries() + 1);
		} else {
			shipper.setFailedDeliveries(shipper.getFailedDeliveries() + 1);
		}

		shipperRepository.save(shipper);
	}

	// ShipmentService.java - Thêm method mới
	public List<Shipper> getAllShippers() {
		return shipperRepository.findAll();
	}

	public Shipper getShipperById(Long id) {
		return shipperRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Shipper not found with id: " + id));
	}

	public Shipper getShipperByUserId(Long userId) {
		return shipperRepository.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("Shipper not found for user id: " + userId));
	}

	public List<Shipper> getActiveShippers() {
		return shipperRepository.findByIsActive(true);
	}

	@Transactional
	public Shipper updateShipperLocation(Long shipperId, Double latitude, Double longitude) {
		Shipper shipper = getShipperById(shipperId);
		shipper.setCurrentLatitude(latitude);
		shipper.setCurrentLongitude(longitude);
		return shipperRepository.save(shipper);
	}
}