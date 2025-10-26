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
	private final WarehouseRepository warehouseRepository;
	private final PackageConfirmationService packageConfirmationService;
	private final CustomerOrderService customerOrderService;
	private final InboundReceiptRepository inboundReceiptRepository;
	private final InboundReceiptDetailRepository inboundReceiptDetailRepository;
	private final InventoryRepository inventoryRepository;
	private final OutboundReceiptRepository outboundReceiptRepository;
	private final OutboundReceiptDetailRepository outboundReceiptDetailRepository;

	@Transactional
	public Order createOrderFromCustomerOrder(CustomerOrder customerOrder) {
		Warehouse fromWarehouse = warehouseRepository.findByCode(customerOrder.getFromWarehouseCode())
				.orElseThrow(() -> new RuntimeException("From warehouse not found"));
		Warehouse toWarehouse = warehouseRepository.findByCode(customerOrder.getToWarehouseCode())
				.orElseThrow(() -> new RuntimeException("To warehouse not found"));

		Order order = Order.builder()
				.orderCode("ORD" + System.currentTimeMillis())
				.senderName(customerOrder.getSenderName())
				.senderPhone(customerOrder.getSenderPhone())
				.senderAddress(customerOrder.getSenderAddress())
				.recipientName(customerOrder.getRecipientName())
				.recipientPhone(customerOrder.getRecipientPhone())
				.recipientAddress(customerOrder.getRecipientAddress())
				.shipmentFee(customerOrder.getEstimatedFee())
				.notes(customerOrder.getNotes())
				.warehouse(fromWarehouse)
				.destinationWarehouse(toWarehouse)
				.isConfirmed(false)
				.build();

		Order savedOrder = orderRepository.save(order);

		// Create package confirmations
		String[] descriptions = customerOrder.getPackageDescription().split(";");
		for (int i = 0; i < descriptions.length; i++) {
			PackageConfirmation confirmation = PackageConfirmation.builder()
					.order(savedOrder)
					.packageCode("PKG" + savedOrder.getId() + "-" + (i + 1))
					.description(descriptions[i].trim())
					.weight(customerOrder.getTotalWeight() / descriptions.length)
					.length(0.0)
					.width(0.0)
					.height(0.0)
					.unitQuantity(1)
					.build();
			packageConfirmationService.createConfirmation(confirmation);
		}

		customerOrderService.markAsProcessed(customerOrder.getId(), savedOrder.getId());

		notificationService.createNotification("WAREHOUSE", fromWarehouse.getId(),
				"Đơn hàng mới từ khách: " + savedOrder.getOrderCode(),
				NotificationType.ORDER_CREATED, savedOrder);

		return savedOrder;
	}

	// OrderService.java - Sửa method createInboundForOrder
	@Transactional
	private void createInboundForOrder(Order order, User user) {
		List<Package> packages = packageRepository.findByOrderId(order.getId());

		if (packages.isEmpty()) {
			throw new RuntimeException("Không có kiện hàng nào");
		}

		InboundReceipt receipt = InboundReceipt.builder()
				.receiptCode("IB-" + order.getOrderCode() + "-" + System.currentTimeMillis())
				.warehouse(order.getWarehouse())
				.order(order)
				.receivedBy(user)
				.receivedDate(LocalDateTime.now())
				.status(ReceiptStatus.APPROVED)
				.notes("Nhập kho từ đơn " + order.getOrderCode())
				.build();

		InboundReceipt savedReceipt = inboundReceiptRepository.save(receipt);

		for (Package pkg : packages) {
			InboundReceiptDetail detail = InboundReceiptDetail.builder()
					.inboundReceipt(savedReceipt)
					.packageItem(pkg)
					.quantity(pkg.getUnitQuantity() != null ? pkg.getUnitQuantity() : 1)
					.notes("Nhập kho tự động")
					.build();
			inboundReceiptDetailRepository.save(detail);

			Inventory inventory = inventoryRepository
					.findByWarehouseIdAndPackageItemId(order.getWarehouse().getId(), pkg.getId())
					.orElse(Inventory.builder()
							.warehouse(order.getWarehouse())
							.packageItem(pkg)
							.quantity(0)
							.deliveredQuantity(0)
							.remainingQuantity(0)
							.build());

			int qty = pkg.getUnitQuantity() != null ? pkg.getUnitQuantity() : 1;
			inventory.setQuantity(inventory.getQuantity() + qty);
			inventory.setRemainingQuantity(inventory.getRemainingQuantity() + qty);
			inventoryRepository.save(inventory);

			pkg.setStatus(PackageStatus.KHO);
			packageRepository.save(pkg);
		}

		Warehouse warehouse = order.getWarehouse();
		Integer totalStock = inventoryRepository.getTotalRemainingQuantityByWarehouseId(warehouse.getId());
		warehouse.setCurrentStock(totalStock != null ? totalStock : 0);
		warehouseRepository.save(warehouse);
	}

	@Transactional
	public void autoAssignShipper(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found"));

		if (order.getStatus() != OrderStatus.CHO_GIAO) {
			throw new RuntimeException("Đơn hàng không ở trạng thái chờ giao");
		}

		if (!order.getIsConfirmed()) {
			throw new RuntimeException("Đơn hàng chưa được xác nhận");
		}

		List<Package> packages = packageRepository.findByOrderId(orderId);
		if (packages.isEmpty()) {
			throw new RuntimeException("Đơn hàng chưa có kiện hàng");
		}

		for (Package pkg : packages) {
			Inventory inventory = inventoryRepository
					.findByWarehouseIdAndPackageItemId(order.getWarehouse().getId(), pkg.getId())
					.orElse(null);

			if (inventory == null
					|| inventory.getRemainingQuantity() < (pkg.getUnitQuantity() != null ? pkg.getUnitQuantity() : 1)) {
				throw new RuntimeException(
						"Kiện hàng " + pkg.getPackageCode() + " chưa nhập kho hoặc không đủ số lượng");
			}
		}

		List<Shipper> allShippers = shipperRepository.findAll();
		if (allShippers.isEmpty()) {
			throw new RuntimeException("Không có shipper nào trong hệ thống");
		}

		Shipper shipper = allShippers.get(0);

		createOutboundForOrder(order, shipper);

		order.setShipper(shipper);
		order.setStatus(OrderStatus.DANG_GIAO);
		orderRepository.save(order);

		Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
		if (shipment == null) {
			shipment = Shipment.builder()
					.shipmentCode("SH" + System.currentTimeMillis())
					.order(order)
					.shipper(shipper)
					.status(ShipmentStatus.IN_TRANSIT)
					.build();
			shipment = shipmentRepository.save(shipment);
			shipmentLegService.createShipmentLegs(shipment, order);
		} else {
			shipment.setShipper(shipper);
			shipment.setStatus(ShipmentStatus.IN_TRANSIT);
			shipmentRepository.save(shipment);
		}

		ShipmentLeg firstLeg = shipmentLegRepository
				.findFirstByShipmentIdAndStatusOrderByLegSequence(shipment.getId(), ShipmentStatus.PENDING)
				.orElse(null);

		if (firstLeg != null) {
			firstLeg.setShipper(shipper);
			firstLeg.setStatus(ShipmentStatus.IN_TRANSIT);
			shipmentLegRepository.save(firstLeg);
		}

		notificationService.createNotification("SHIPPER", shipper.getId(),
				"Đơn hàng mới: " + order.getOrderCode() + " đã được phân công",
				NotificationType.ORDER_ASSIGNED, order);

		notificationService.createNotification("WAREHOUSE", order.getWarehouse().getId(),
				"Đơn hàng " + order.getOrderCode() + " đã xuất kho và phân công cho " + shipper.getName(),
				NotificationType.ORDER_ASSIGNED, order);
	}

	@Transactional
	private void createOutboundForOrder(Order order, Shipper shipper) {
		List<Package> packages = packageRepository.findByOrderId(order.getId());

		if (packages.isEmpty()) {
			throw new RuntimeException("Không có kiện hàng nào");
		}

		OutboundReceipt receipt = OutboundReceipt.builder()
				.receiptCode("OB-" + order.getOrderCode() + "-" + System.currentTimeMillis())
				.warehouse(order.getWarehouse())
				.order(order)
				.shipper(shipper)
				.issuedBy(order.getWarehouse().getUser())
				.issuedDate(LocalDateTime.now())
				.status(ReceiptStatus.APPROVED)
				.notes("Xuất kho giao cho " + shipper.getName())
				.build();

		OutboundReceipt savedReceipt = outboundReceiptRepository.save(receipt);

		for (Package pkg : packages) {
			int qty = pkg.getUnitQuantity() != null ? pkg.getUnitQuantity() : 1;

			Inventory inventory = inventoryRepository
					.findByWarehouseIdAndPackageItemId(order.getWarehouse().getId(), pkg.getId())
					.orElseThrow(() -> new RuntimeException("Kiện hàng " + pkg.getPackageCode() + " chưa nhập kho"));

			if (inventory.getRemainingQuantity() < qty) {
				throw new RuntimeException("Không đủ hàng trong kho cho kiện " + pkg.getPackageCode() +
						". Cần: " + qty + ", Còn: " + inventory.getRemainingQuantity());
			}

			OutboundReceiptDetail detail = OutboundReceiptDetail.builder()
					.outboundReceipt(savedReceipt)
					.packageItem(pkg)
					.quantity(qty)
					.notes("Xuất kho tự động cho giao hàng")
					.build();
			outboundReceiptDetailRepository.save(detail);

			inventory.setDeliveredQuantity(inventory.getDeliveredQuantity() + qty);
			inventory.setRemainingQuantity(inventory.getRemainingQuantity() - qty);
			inventoryRepository.save(inventory);

			pkg.setStatus(PackageStatus.DANG_VAN_CHUYEN);
			packageRepository.save(pkg);
		}

		Warehouse warehouse = order.getWarehouse();
		Integer totalStock = inventoryRepository.getTotalRemainingQuantityByWarehouseId(warehouse.getId());
		warehouse.setCurrentStock(totalStock != null ? totalStock : 0);
		warehouseRepository.save(warehouse);
	}

	public List<Order> getUnconfirmedOrders(Long warehouseId) {
		return orderRepository.findByIsConfirmedFalseAndWarehouseId(warehouseId);
	}

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

	public List<Order> getOrdersByDestinationWarehouseId(Long destinationWarehouseId) {
		return orderRepository.findByDestinationWarehouseId(destinationWarehouseId);
	}

	public List<Order> getOrdersByDestinationWarehouseAndStatus(Long destinationWarehouseId, OrderStatus status) {
		return orderRepository.findByDestinationWarehouseIdAndStatus(destinationWarehouseId, status);
	}

	public long countOrdersByDestinationWarehouseAndStatus(Long destinationWarehouseId, OrderStatus status) {
		return orderRepository.countByDestinationWarehouseIdAndStatus(destinationWarehouseId, status);
	}

	@Transactional
	public Order confirmOrder(Long orderId, User confirmedBy) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found"));

		if (packageConfirmationService.hasUnconfirmedPackages(orderId)) {
			throw new RuntimeException("Vẫn còn kiện hàng chưa xác nhận");
		}

		order.setIsConfirmed(true);
		order.setConfirmedBy(confirmedBy.getId());
		order.setConfirmedAt(LocalDateTime.now());
		order.setStatus(OrderStatus.CHO_GIAO);
		Order savedOrder = orderRepository.save(order);

		createInboundForOrder(savedOrder, confirmedBy);

		notificationService.createNotification("WAREHOUSE", order.getWarehouse().getId(),
				"Đơn hàng " + order.getOrderCode() + " đã nhập kho và sẵn sàng để giao",
				NotificationType.ORDER_CREATED, savedOrder);

		return savedOrder;
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
		order.setStatus(status);
		Order savedOrder = orderRepository.save(order);

		Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
		if (shipment != null) {
			if (status == OrderStatus.DANG_GIAO) {
				shipment.setStatus(ShipmentStatus.IN_TRANSIT);
			} else if (status == OrderStatus.HOAN_THANH) {
				shipment.setStatus(ShipmentStatus.DELIVERED);
			} else if (status == OrderStatus.THAT_BAI) {
				shipment.setStatus(ShipmentStatus.FAILED);
			}
			shipmentRepository.save(shipment);
		}
		if (status == OrderStatus.HOAN_THANH) {
			if (order.getWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE", order.getWarehouse().getId(),
						"Order completed: " + order.getOrderCode(), NotificationType.ORDER_COMPLETED, savedOrder);
			}

			if (order.getDestinationWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE",
						order.getDestinationWarehouse().getId(),
						"Order completed: " + order.getOrderCode(),
						NotificationType.ORDER_COMPLETED, savedOrder);
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

			if (order.getDestinationWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE",
						order.getDestinationWarehouse().getId(),
						"Order failed: " + order.getOrderCode(),
						NotificationType.ORDER_FAILED, savedOrder);
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