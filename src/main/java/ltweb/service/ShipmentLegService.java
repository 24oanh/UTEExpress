package ltweb.service;

import ltweb.entity.*;
import ltweb.repository.OrderRepository;
import ltweb.repository.ShipmentLegRepository;
import ltweb.repository.ShipmentRepository;
import ltweb.repository.ShipperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentLegService {

	private final ShipmentLegRepository shipmentLegRepository;
	private final RouteCalculationService routeCalculationService;
	private final NotificationService notificationService;
	private final OrderRepository orderRepository;
	private final ShipmentRepository shipmentRepository;
	private final ShipperRepository shipperRepository;

	@Transactional
	public void createShipmentLegs(Shipment shipment, Order order) {
		List<RouteCalculationService.RouteSegment> route = routeCalculationService.calculateRoute(order.getWarehouse(),
				order.getDestinationWarehouse());

		for (int i = 0; i < route.size(); i++) {
			RouteCalculationService.RouteSegment segment = route.get(i);

			ShipmentLeg leg = ShipmentLeg.builder().shipment(shipment).order(order)
					.fromWarehouse(segment.getFromWarehouse()).toWarehouse(segment.getToWarehouse())
					.shipper(segment.getPreferredShipper()).legOrder(i + 1).legSequence(i + 1)
					.distanceKm(segment.getDistanceKm()).estimatedHours(segment.getEstimatedHours())
					.isFinalLeg(segment.getIsFinalLeg())
					.status(i == 0 ? ShipmentStatus.PENDING : ShipmentStatus.PENDING).build();

			shipmentLegRepository.save(leg);
		}
	}

	public RouteCalculationService.RouteSegment getFirstLegPreferredShipper(Order order) {
		List<RouteCalculationService.RouteSegment> route = routeCalculationService.calculateRoute(order.getWarehouse(),
				order.getDestinationWarehouse());
		return route.isEmpty() ? null : route.get(0);
	}

	// ShipmentLegService.java - Thêm methods mới
	// ShipmentLegService.java - Thay thế method completeCurrentLegAndStartNext
	@Transactional
	public void completeCurrentLegAndStartNext(Long shipmentId) {
		ShipmentLeg currentLeg = shipmentLegRepository
				.findFirstByShipmentIdAndStatusOrderByLegSequence(shipmentId, ShipmentStatus.IN_TRANSIT)
				.orElse(shipmentLegRepository
						.findFirstByShipmentIdAndStatusOrderByLegSequence(shipmentId, ShipmentStatus.PENDING)
						.orElse(null));

		if (currentLeg == null)
			return;

		currentLeg.setStatus(ShipmentStatus.DELIVERED);
		currentLeg.setDeliveryTime(LocalDateTime.now());
		shipmentLegRepository.save(currentLeg);

		// Thông báo warehouse nhận hàng (nếu không phải chặng cuối)
		if (!currentLeg.getIsFinalLeg() && currentLeg.getToWarehouse() != null) {
			notificationService.createNotification("WAREHOUSE",
					currentLeg.getToWarehouse().getId(),
					"Đơn hàng " + currentLeg.getOrder().getOrderCode() + " đã đến kho",
					NotificationType.ORDER_ASSIGNED, currentLeg.getOrder());
		}

		ShipmentLeg nextLeg = shipmentLegRepository
				.findByShipmentIdAndLegSequence(shipmentId, currentLeg.getLegSequence() + 1)
				.orElse(null);

		if (nextLeg != null) {
			nextLeg.setStatus(ShipmentStatus.PENDING);
			shipmentLegRepository.save(nextLeg);

			// Thông báo shipper tiếp theo
			if (nextLeg.getShipper() != null) {
				notificationService.createNotification("SHIPPER",
						nextLeg.getShipper().getId(),
						"Bạn có chặng mới: " + nextLeg.getOrder().getOrderCode() +
								" từ " + nextLeg.getFromWarehouse().getName() +
								(nextLeg.getIsFinalLeg() ? " giao cho khách hàng"
										: " đến " + nextLeg.getToWarehouse().getName()),
						NotificationType.NEW_DELIVERY, nextLeg.getOrder());
			}

			// Thông báo warehouse xuất hàng
			notificationService.createNotification("WAREHOUSE",
					nextLeg.getFromWarehouse().getId(),
					"Cần xuất hàng cho đơn " + nextLeg.getOrder().getOrderCode(),
					NotificationType.ORDER_ASSIGNED, nextLeg.getOrder());

		} else {
			// Hoàn thành toàn bộ
			Order order = currentLeg.getOrder();
			order.setStatus(OrderStatus.HOAN_THANH);
			orderRepository.save(order);

			Shipment shipment = currentLeg.getShipment();
			shipment.setStatus(ShipmentStatus.DELIVERED);
			shipment.setDeliveryTime(LocalDateTime.now());
			shipmentRepository.save(shipment);

			// Thông báo hoàn thành cho cả 2 kho
			if (order.getWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE",
						order.getWarehouse().getId(),
						"Đơn hàng " + order.getOrderCode() + " đã giao thành công",
						NotificationType.ORDER_COMPLETED, order);
			}
			if (order.getDestinationWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE",
						order.getDestinationWarehouse().getId(),
						"Đơn hàng " + order.getOrderCode() + " đã được giao đến khách hàng",
						NotificationType.DELIVERY_COMPLETED, order);
			}
		}
	}

	@Transactional
	public void failCurrentLeg(Long shipmentId, String notes) {
		ShipmentLeg currentLeg = shipmentLegRepository
				.findFirstByShipmentIdAndStatusOrderByLegSequence(shipmentId, ShipmentStatus.IN_TRANSIT)
				.orElse(null);

		if (currentLeg == null)
			return;

		currentLeg.setStatus(ShipmentStatus.FAILED);
		currentLeg.setNotes(notes);
		shipmentLegRepository.save(currentLeg);

		Shipment shipment = currentLeg.getShipment();
		shipment.setStatus(ShipmentStatus.FAILED);
		shipment.setNotes(notes);
		shipmentRepository.save(shipment);

		Order order = currentLeg.getOrder();
		order.setStatus(OrderStatus.THAT_BAI);
		orderRepository.save(order);

		// Gửi thông báo đến warehouse hiện tại
		if (currentLeg.getFromWarehouse() != null) {
			notificationService.createNotification("WAREHOUSE",
					currentLeg.getFromWarehouse().getId(),
					"Giao hàng thất bại: " + order.getOrderCode() + " - " + notes,
					NotificationType.ORDER_FAILED, order);
		}

		// Gửi thông báo đến warehouse đích
		if (order.getDestinationWarehouse() != null) {
			notificationService.createNotification("WAREHOUSE",
					order.getDestinationWarehouse().getId(),
					"Đơn hàng " + order.getOrderCode() + " giao thất bại",
					NotificationType.ORDER_FAILED, order);
		}
	}

	@Transactional
	public void reassignLeg(Long legId, Long newShipperId) {
		ShipmentLeg leg = shipmentLegRepository.findById(legId)
				.orElseThrow(() -> new RuntimeException("Leg not found"));

		Shipper newShipper = shipperRepository.findById(newShipperId)
				.orElseThrow(() -> new RuntimeException("Shipper not found"));

		leg.setShipper(newShipper);
		leg.setStatus(ShipmentStatus.PENDING);
		leg.setNotes(null);
		shipmentLegRepository.save(leg);

		Shipment shipment = leg.getShipment();
		shipment.setShipper(newShipper);
		shipment.setStatus(ShipmentStatus.PENDING);
		shipmentRepository.save(shipment);

		Order order = leg.getOrder();
		order.setShipper(newShipper);
		order.setStatus(OrderStatus.CHO_GIAO);
		orderRepository.save(order);

		notificationService.createNotification("SHIPPER", newShipperId,
				"Bạn được phân công chặng: " + order.getOrderCode(),
				NotificationType.ORDER_ASSIGNED, order);
	}
}