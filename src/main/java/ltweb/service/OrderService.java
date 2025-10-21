package ltweb.service;

import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final PackageRepository packageRepository;
	private final ShipperRepository shipperRepository;
	private final NotificationService notificationService;
	private final ShipmentRepository shipmentRepository;
	private final ShipmentLegService shipmentLegService;
	private final ShipmentLegRepository shipmentLegRepository;

	@Transactional
	public Order createOrder(Order order) {
		Order savedOrder = orderRepository.save(order);

		if (savedOrder.getWarehouse() != null) {
			notificationService.createNotification("WAREHOUSE", savedOrder.getWarehouse().getId(),
					"New order created: " + savedOrder.getOrderCode(), NotificationType.ORDER_CREATED, savedOrder);
		}

		if (savedOrder.getWarehouse() != null && savedOrder.getDestinationWarehouse() != null) {
			createShipmentWithLegs(savedOrder);
		}

		return savedOrder;
	}

	private void createShipmentWithLegs(Order order) {
		RouteCalculationService.RouteSegment firstSegment = shipmentLegService.getFirstLegPreferredShipper(order);

		Shipper defaultShipper = null;
		if (firstSegment != null && firstSegment.getPreferredShipper() != null) {
			defaultShipper = firstSegment.getPreferredShipper();
		}

		Shipment shipment = Shipment.builder().shipmentCode("SH" + System.currentTimeMillis()).order(order)
				.shipper(defaultShipper).status(ShipmentStatus.PENDING).build();

		shipment = shipmentRepository.save(shipment);

		shipmentLegService.createShipmentLegs(shipment, order);

		if (defaultShipper != null) {
			order.setShipper(defaultShipper);
			orderRepository.save(order);

			notificationService.createNotification("SHIPPER", defaultShipper.getId(),
					"New order assigned: " + order.getOrderCode(), NotificationType.ORDER_ASSIGNED, order);
		}
	}

	@Transactional
	public Order assignOrderToShipper(Long orderId, Long shipperId) {
		Order order = getOrderById(orderId);
		Shipper shipper = shipperRepository.findById(shipperId)
				.orElseThrow(() -> new RuntimeException("Shipper not found"));

		order.setShipper(shipper);
		order.setStatus(OrderStatus.CHO_GIAO);
		Order savedOrder = orderRepository.save(order);

		Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
		if (shipment != null) {
			shipment.setShipper(shipper);
			shipmentRepository.save(shipment);

			ShipmentLeg firstLeg = shipmentLegRepository
					.findFirstByShipmentIdAndStatusOrderByLegOrder(shipment.getId(), ShipmentStatus.PENDING)
					.orElse(null);

			if (firstLeg != null) {
				firstLeg.setShipper(shipper);
				shipmentLegRepository.save(firstLeg);
			}
		}

		notificationService.createNotification("SHIPPER", shipperId, "New order assigned: " + order.getOrderCode(),
				NotificationType.ORDER_ASSIGNED, savedOrder);

		if (order.getWarehouse() != null) {
			notificationService.createNotification("WAREHOUSE", order.getWarehouse().getId(),
					"Order " + order.getOrderCode() + " assigned to " + shipper.getName(),
					NotificationType.ORDER_ASSIGNED, savedOrder);
		}

		return savedOrder;
	}

	public List<Order> getAllOrders() {
		return orderRepository.findAll();
	}

	public Order getOrderById(Long id) {
		return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
	}

	public Order getOrderByCode(String orderCode) {
		return orderRepository.findByOrderCode(orderCode)
				.orElseThrow(() -> new RuntimeException("Order not found with code: " + orderCode));
	}

	public List<Order> getOrdersByWarehouseId(Long warehouseId) {
		return orderRepository.findByWarehouseId(warehouseId);
	}

	public List<Order> getOrdersByShipperId(Long shipperId) {
		return orderRepository.findByShipperId(shipperId);
	}

	public List<Order> getOrdersByStatus(OrderStatus status) {
		return orderRepository.findByStatus(status);
	}

	public List<Order> getOrdersByWarehouseAndStatus(Long warehouseId, OrderStatus status) {
		return orderRepository.findByWarehouseIdAndStatus(warehouseId, status);
	}

	public List<Order> getOrdersByShipperAndStatus(Long shipperId, OrderStatus status) {
		return orderRepository.findByShipperIdAndStatus(shipperId, status);
	}

	@Transactional
	public Order updateOrder(Long id, Order orderDetails) {
		Order order = getOrderById(id);
		order.setSenderName(orderDetails.getSenderName());
		order.setSenderPhone(orderDetails.getSenderPhone());
		order.setSenderAddress(orderDetails.getSenderAddress());
		order.setRecipientName(orderDetails.getRecipientName());
		order.setRecipientPhone(orderDetails.getRecipientPhone());
		order.setRecipientAddress(orderDetails.getRecipientAddress());
		order.setShipmentFee(orderDetails.getShipmentFee());
		order.setNotes(orderDetails.getNotes());
		return orderRepository.save(order);
	}

	@Transactional
	public Order updateOrderStatus(Long orderId, OrderStatus status) {
		Order order = getOrderById(orderId);
		OrderStatus oldStatus = order.getStatus();
		order.setStatus(status);
		Order savedOrder = orderRepository.save(order);

		if (status == OrderStatus.HOAN_THANH) {
			if (order.getWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE", order.getWarehouse().getId(),
						"Order completed: " + order.getOrderCode(), NotificationType.ORDER_COMPLETED, savedOrder);
			}

			List<Package> packages = packageRepository.findByOrderId(orderId);
			for (Package pkg : packages) {
				pkg.setStatus(PackageStatus.DA_GIAO);
				packageRepository.save(pkg);
			}
		} else if (status == OrderStatus.THAT_BAI) {
			if (order.getWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE", order.getWarehouse().getId(),
						"Order failed: " + order.getOrderCode(), NotificationType.ORDER_FAILED, savedOrder);
			}
		}

		return savedOrder;
	}

	@Transactional
	public Order cancelOrder(Long orderId) {
		Order order = getOrderById(orderId);
		if (order.getStatus() != OrderStatus.CHO_GIAO) {
			throw new RuntimeException("Cannot cancel order in current status");
		}
		order.setStatus(OrderStatus.HUY);
		return orderRepository.save(order);
	}

	@Transactional
	public Package addPackageToOrder(Long orderId, Package packageItem) {
		Order order = getOrderById(orderId);
		packageItem.setOrder(order);
		return packageRepository.save(packageItem);
	}

	public List<Package> getPackagesByOrderId(Long orderId) {
		return packageRepository.findByOrderId(orderId);
	}

	public long countOrdersByWarehouseAndStatus(Long warehouseId, OrderStatus status) {
		return orderRepository.countByWarehouseIdAndStatus(warehouseId, status);
	}

	public List<Order> getOrdersByWarehouseAndDateRange(Long warehouseId, LocalDateTime startDate,
			LocalDateTime endDate) {
		return orderRepository.findByWarehouseIdAndDateRange(warehouseId, startDate, endDate);
	}
}