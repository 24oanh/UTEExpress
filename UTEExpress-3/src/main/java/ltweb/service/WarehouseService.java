package ltweb.service;

import ltweb.dto.InboundOutboundReportDTO;
import ltweb.dto.InboundReceiptDTO;
import ltweb.dto.InboundReceiptDetailDTO;
import ltweb.dto.InventoryDTO;
import ltweb.dto.InventoryReportDTO;
import ltweb.dto.OutboundReceiptDTO;
import ltweb.dto.OutboundReceiptDetailDTO;
import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

	private final WarehouseRepository warehouseRepository;
	private final InventoryRepository inventoryRepository;
	private final InboundReceiptRepository inboundReceiptRepository;
	private final InboundReceiptDetailRepository inboundReceiptDetailRepository;
	private final OutboundReceiptRepository outboundReceiptRepository;
	private final OutboundReceiptDetailRepository outboundReceiptDetailRepository;
	private final WarehouseLocationRepository warehouseLocationRepository;
	private final WarehouseStatusHistoryRepository warehouseStatusHistoryRepository;
	private final PackageRepository packageRepository;
	private final ShipmentRepository shipmentRepository;
	private final TrackingService trackingService;
	private final OrderRepository orderRepository;
	private final ShipperRepository shipperRepository;
	private final NotificationService notificationService;
	

	public List<Warehouse> getAllWarehouses() {
		return warehouseRepository.findAll();
	}

	public Warehouse getWarehouseById(Long id) {
		return warehouseRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + id));
	}

	public Warehouse getWarehouseByCode(String code) {
		return warehouseRepository.findByCode(code)
				.orElseThrow(() -> new RuntimeException("Warehouse not found with code: " + code));
	}

	public Warehouse getWarehouseByUserId(Long userId) {
		return warehouseRepository.findByUserId(userId)
				.orElseThrow(() -> new RuntimeException("Warehouse not found for user id: " + userId));
	}

	// Thêm vào WarehouseService.java

	public List<InboundReceipt> getInboundReceiptsByOrderId(Long orderId) {
		return inboundReceiptRepository.findByOrderId(orderId);
	}

	public List<OutboundReceipt> getOutboundReceiptsByOrderId(Long orderId) {
		return outboundReceiptRepository.findByOrderId(orderId);
	}

	public Inventory getInventoryByPackageId(Long warehouseId, Long packageId) {
		return inventoryRepository.findByWarehouseIdAndPackageItemId(warehouseId, packageId)
				.orElse(null);
	}

	// Low stock alert
	public List<Inventory> getLowStockItems(Long warehouseId, Integer threshold) {
		return inventoryRepository.findByWarehouseId(warehouseId).stream()
				.filter(inv -> inv.getRemainingQuantity() <= threshold)
				.collect(Collectors.toList());
	}

	// Inventory value
	public Double getTotalInventoryValue(Long warehouseId) {
		List<Inventory> inventories = inventoryRepository.findByWarehouseId(warehouseId);
		return inventories.stream()
				.mapToDouble(inv -> {
					Order order = inv.getPackageItem().getOrder();
					return order.getShipmentFee().doubleValue() * inv.getRemainingQuantity();
				})
				.sum();
	}

	@Transactional
	public Warehouse createWarehouse(Warehouse warehouse) {
		if (warehouseRepository.existsByCode(warehouse.getCode())) {
			throw new RuntimeException("Warehouse code already exists");
		}
		return warehouseRepository.save(warehouse);
	}

	@Transactional
	public Warehouse updateWarehouse(Long id, Warehouse warehouseDetails) {
		Warehouse warehouse = getWarehouseById(id);
		warehouse.setName(warehouseDetails.getName());
		warehouse.setAddress(warehouseDetails.getAddress());
		warehouse.setPhone(warehouseDetails.getPhone());
		warehouse.setEmail(warehouseDetails.getEmail());
		warehouse.setManager(warehouseDetails.getManager());
		warehouse.setTotalCapacity(warehouseDetails.getTotalCapacity());
		return warehouseRepository.save(warehouse);
	}

	public List<Inventory> getInventoryByWarehouseId(Long warehouseId) {
		return inventoryRepository.findByWarehouseId(warehouseId);
	}

	public List<Inventory> getAvailableInventory(Long warehouseId) {
		return inventoryRepository.findAvailableInventoryByWarehouseId(warehouseId);
	}

	// NHẬP KHO - Khi nhận hàng từ customer
	@Transactional
	public InboundReceipt receiveOrderToWarehouse(Long orderId, Long warehouseId, User receivedBy) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found"));

		Warehouse warehouse = warehouseRepository.findById(warehouseId)
				.orElseThrow(() -> new RuntimeException("Warehouse not found"));

		// Tạo inbound receipt
		InboundReceipt receipt = InboundReceipt.builder()
				.receiptCode("IBR" + System.currentTimeMillis())
				.warehouse(warehouse)
				.order(order)
				.receivedBy(receivedBy)
				.receivedDate(LocalDateTime.now())
				.status(ReceiptStatus.APPROVED)
				.notes("Receive order from customer: " + order.getOrderCode())
				.build();

		receipt = inboundReceiptRepository.save(receipt);

		// Lấy tất cả packages của order
		List<Package> packages = packageRepository.findByOrderId(orderId);

		for (Package pkg : packages) {
			// Tạo inbound detail
			InboundReceiptDetail detail = InboundReceiptDetail.builder()
					.inboundReceipt(receipt)
					.packageItem(pkg)
					.quantity(pkg.getUnitQuantity())
					.notes("Receive package: " + pkg.getPackageCode())
					.build();

			inboundReceiptDetailRepository.save(detail);

			// Cập nhật inventory
			updateInventoryOnInbound(warehouse.getId(), pkg.getId(), pkg.getUnitQuantity());

			// Cập nhật package status
			pkg.setStatus(PackageStatus.KHO);
			packageRepository.save(pkg);

			// Tracking
			Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
			if (shipment != null) {
				trackingService.createTracking(
						shipment, 0.0, 0.0,
						"Package " + pkg.getPackageCode() + " received at warehouse " + warehouse.getName(),
						TrackingStatus.IN_PROGRESS);
			}
		}

		// Cập nhật warehouse current stock
		updateWarehouseCurrentStock(warehouse.getId());

		// Notification
		if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
			notificationService.createNotification(
					order.getCustomer().getUser().getId(),
					"CUSTOMER",
					"Đơn hàng " + order.getOrderCode() + " đã được nhận vào kho " + warehouse.getName(),
					NotificationType.ORDER_ASSIGNED,
					order);
		}

		return receipt;
	}

	// XUẤT KHO - Khi giao hàng cho shipper
	@Transactional
	public OutboundReceipt issueOrderToShipper(Long orderId, Long shipperId, User issuedBy) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found"));

		Shipper shipper = shipperRepository.findById(shipperId)
				.orElseThrow(() -> new RuntimeException("Shipper not found"));

		Warehouse warehouse = order.getWarehouse();

		// Tạo outbound receipt
		OutboundReceipt receipt = OutboundReceipt.builder()
				.receiptCode("OBR" + System.currentTimeMillis())
				.warehouse(warehouse)
				.order(order)
				.shipper(shipper)
				.issuedBy(issuedBy)
				.issuedDate(LocalDateTime.now())
				.status(ReceiptStatus.APPROVED)
				.notes("Issue order to shipper: " + shipper.getName())
				.build();

		receipt = outboundReceiptRepository.save(receipt);

		// Lấy tất cả packages của order
		List<Package> packages = packageRepository.findByOrderId(orderId);

		for (Package pkg : packages) {
			// Kiểm tra tồn kho
			Inventory inventory = inventoryRepository
					.findByWarehouseIdAndPackageItemId(warehouse.getId(), pkg.getId())
					.orElseThrow(() -> new RuntimeException("Package not in warehouse inventory"));

			if (inventory.getRemainingQuantity() < pkg.getUnitQuantity()) {
				throw new RuntimeException("Insufficient inventory for package: " + pkg.getPackageCode());
			}

			// Tạo outbound detail
			OutboundReceiptDetail detail = OutboundReceiptDetail.builder()
					.outboundReceipt(receipt)
					.packageItem(pkg)
					.quantity(pkg.getUnitQuantity())
					.notes("Issue package: " + pkg.getPackageCode())
					.build();

			outboundReceiptDetailRepository.save(detail);

			// Cập nhật inventory
			updateInventoryOnOutbound(warehouse.getId(), pkg.getId(), pkg.getUnitQuantity());

			// Cập nhật package status
			pkg.setStatus(PackageStatus.DANG_VAN_CHUYEN);
			packageRepository.save(pkg);

			// Tracking
			Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
			if (shipment != null) {
				trackingService.createTracking(
						shipment, 0.0, 0.0,
						"Package " + pkg.getPackageCode() + " issued from warehouse to shipper " + shipper.getName(),
						TrackingStatus.IN_PROGRESS);
			}
		}

		// Cập nhật warehouse current stock
		updateWarehouseCurrentStock(warehouse.getId());

		// Assign shipper to order
		order.setShipper(shipper);
		orderRepository.save(order);

		// Notification cho shipper
		if (shipper.getUser() != null) {
			notificationService.createNotification(
					shipper.getUser().getId(),
					"SHIPPER",
					"Bạn được giao đơn hàng " + order.getOrderCode() + " từ kho " + warehouse.getName(),
					NotificationType.ORDER_ASSIGNED,
					order);
		}

		// Notification cho customer
		if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
			notificationService.createNotification(
					order.getCustomer().getUser().getId(),
					"CUSTOMER",
					"Đơn hàng " + order.getOrderCode() + " đã được xuất kho và shipper đang trên đường giao",
					NotificationType.ORDER_ASSIGNED,
					order);
		}

		return receipt;
	}

	// Helper: Update inventory on inbound
	@Transactional
	private void updateInventoryOnInbound(Long warehouseId, Long packageId, Integer quantity) {
		Inventory inventory = inventoryRepository
				.findByWarehouseIdAndPackageItemId(warehouseId, packageId)
				.orElse(Inventory.builder()
						.warehouse(warehouseRepository.findById(warehouseId).orElseThrow())
						.packageItem(packageRepository.findById(packageId).orElseThrow())
						.quantity(0)
						.deliveredQuantity(0)
						.remainingQuantity(0)
						.build());

		inventory.setQuantity(inventory.getQuantity() + quantity);
		inventory.setRemainingQuantity(inventory.getQuantity() - inventory.getDeliveredQuantity());
		inventoryRepository.save(inventory);
	}

	// Helper: Update inventory on outbound
	@Transactional
	private void updateInventoryOnOutbound(Long warehouseId, Long packageId, Integer quantity) {
		Inventory inventory = inventoryRepository
				.findByWarehouseIdAndPackageItemId(warehouseId, packageId)
				.orElseThrow(() -> new RuntimeException("Inventory not found"));

		if (inventory.getRemainingQuantity() < quantity) {
			throw new RuntimeException("Insufficient inventory");
		}

		inventory.setDeliveredQuantity(inventory.getDeliveredQuantity() + quantity);
		inventory.setRemainingQuantity(inventory.getQuantity() - inventory.getDeliveredQuantity());
		inventoryRepository.save(inventory);
	}

	// Helper: Update warehouse current stock
	@Transactional
	private void updateWarehouseCurrentStock(Long warehouseId) {
		Integer totalRemaining = inventoryRepository
				.getTotalRemainingQuantityByWarehouseId(warehouseId);

		Warehouse warehouse = warehouseRepository.findById(warehouseId)
				.orElseThrow(() -> new RuntimeException("Warehouse not found"));

		warehouse.setCurrentStock(totalRemaining != null ? totalRemaining : 0);
		warehouseRepository.save(warehouse);
	}

	// BÁO CÁO TỒN KHO
	public InventoryReportDTO getInventoryReport(Long warehouseId, LocalDateTime startDate, LocalDateTime endDate) {
		Warehouse warehouse = warehouseRepository.findById(warehouseId)
				.orElseThrow(() -> new RuntimeException("Warehouse not found"));

		// Lấy tất cả inventory hiện tại
		List<Inventory> inventories = inventoryRepository.findByWarehouseId(warehouseId);

		// Tính toán
		Integer totalQuantity = inventories.stream()
				.mapToInt(Inventory::getQuantity)
				.sum();

		Integer totalDelivered = inventories.stream()
				.mapToInt(Inventory::getDeliveredQuantity)
				.sum();

		Integer totalRemaining = inventories.stream()
				.mapToInt(Inventory::getRemainingQuantity)
				.sum();

		// Lấy inbound/outbound trong khoảng thời gian
		List<InboundReceipt> inboundReceipts = inboundReceiptRepository
				.findByWarehouseIdAndDateRange(warehouseId, startDate, endDate);

		List<OutboundReceipt> outboundReceipts = outboundReceiptRepository
				.findByWarehouseIdAndDateRange(warehouseId, startDate, endDate);

		Integer totalInbound = inboundReceipts.stream()
				.flatMap(r -> inboundReceiptDetailRepository.findByInboundReceiptId(r.getId()).stream())
				.mapToInt(InboundReceiptDetail::getQuantity)
				.sum();

		Integer totalOutbound = outboundReceipts.stream()
				.flatMap(r -> outboundReceiptDetailRepository.findByOutboundReceiptId(r.getId()).stream())
				.mapToInt(OutboundReceiptDetail::getQuantity)
				.sum();

		Double utilizationRate = warehouse.getTotalCapacity() != null && warehouse.getTotalCapacity() > 0
				? (totalRemaining.doubleValue() / warehouse.getTotalCapacity()) * 100
				: 0.0;

		return InventoryReportDTO.builder()
				.warehouseId(warehouseId)
				.warehouseName(warehouse.getName())
				.startDate(startDate)
				.endDate(endDate)
				.totalQuantity(totalQuantity)
				.totalDelivered(totalDelivered)
				.totalRemaining(totalRemaining)
				.totalInbound(totalInbound)
				.totalOutbound(totalOutbound)
				.totalCapacity(warehouse.getTotalCapacity())
				.utilizationRate(utilizationRate)
				.inventories(inventories.stream()
						.map(this::convertToInventoryDTO)
						.collect(Collectors.toList()))
				.build();
	}

	// BÁO CÁO NHẬP XUẤT
	public InboundOutboundReportDTO getInboundOutboundReport(Long warehouseId,
			LocalDateTime startDate,
			LocalDateTime endDate) {
		Warehouse warehouse = warehouseRepository.findById(warehouseId)
				.orElseThrow(() -> new RuntimeException("Warehouse not found"));

		List<InboundReceipt> inboundReceipts = inboundReceiptRepository
				.findByWarehouseIdAndDateRange(warehouseId, startDate, endDate);

		List<OutboundReceipt> outboundReceipts = outboundReceiptRepository
				.findByWarehouseIdAndDateRange(warehouseId, startDate, endDate);

		// Group by date
		Map<LocalDate, Integer> inboundByDate = inboundReceipts.stream()
				.collect(Collectors.groupingBy(
						r -> r.getReceivedDate().toLocalDate(),
						Collectors.summingInt(r -> inboundReceiptDetailRepository
								.findByInboundReceiptId(r.getId())
								.stream()
								.mapToInt(InboundReceiptDetail::getQuantity)
								.sum())));

		Map<LocalDate, Integer> outboundByDate = outboundReceipts.stream()
				.collect(Collectors.groupingBy(
						r -> r.getIssuedDate().toLocalDate(),
						Collectors.summingInt(r -> outboundReceiptDetailRepository
								.findByOutboundReceiptId(r.getId())
								.stream()
								.mapToInt(OutboundReceiptDetail::getQuantity)
								.sum())));

		return InboundOutboundReportDTO.builder()
				.warehouseId(warehouseId)
				.warehouseName(warehouse.getName())
				.startDate(startDate)
				.endDate(endDate)
				.totalInboundReceipts(inboundReceipts.size())
				.totalOutboundReceipts(outboundReceipts.size())
				.inboundByDate(inboundByDate)
				.outboundByDate(outboundByDate)
				.inboundReceipts(inboundReceipts.stream()
						.map(this::convertToInboundReceiptDTO)
						.collect(Collectors.toList()))
				.outboundReceipts(outboundReceipts.stream()
						.map(this::convertToOutboundReceiptDTO)
						.collect(Collectors.toList()))
				.build();
	}

	// Converter methods
	private InventoryDTO convertToInventoryDTO(Inventory inventory) {
		return InventoryDTO.builder()
				.id(inventory.getId())
				.packageCode(inventory.getPackageItem().getPackageCode())
				.packageDescription(inventory.getPackageItem().getDescription())
				.quantity(inventory.getQuantity())
				.deliveredQuantity(inventory.getDeliveredQuantity())
				.remainingQuantity(inventory.getRemainingQuantity())
				.orderCode(inventory.getPackageItem().getOrder().getOrderCode())
				.build();
	}

	private InboundReceiptDTO convertToInboundReceiptDTO(InboundReceipt receipt) {
		List<InboundReceiptDetail> details = inboundReceiptDetailRepository
				.findByInboundReceiptId(receipt.getId());

		return InboundReceiptDTO.builder()
				.id(receipt.getId())
				.receiptCode(receipt.getReceiptCode())
				.orderCode(receipt.getOrder() != null ? receipt.getOrder().getOrderCode() : null)
				.receivedBy(receipt.getReceivedBy().getFullName())
				.receivedDate(receipt.getReceivedDate())
				.status(receipt.getStatus())
				.notes(receipt.getNotes())
				.totalQuantity(details.stream().mapToInt(InboundReceiptDetail::getQuantity).sum())
				.details(details.stream()
						.map(d -> InboundReceiptDetailDTO.builder()
								.packageCode(d.getPackageItem().getPackageCode())
								.quantity(d.getQuantity())
								.notes(d.getNotes())
								.build())
						.collect(Collectors.toList()))
				.build();
	}

	private OutboundReceiptDTO convertToOutboundReceiptDTO(OutboundReceipt receipt) {
		List<OutboundReceiptDetail> details = outboundReceiptDetailRepository
				.findByOutboundReceiptId(receipt.getId());

		return OutboundReceiptDTO.builder()
				.id(receipt.getId())
				.receiptCode(receipt.getReceiptCode())
				.orderCode(receipt.getOrder().getOrderCode())
				.shipperName(receipt.getShipper() != null ? receipt.getShipper().getName() : null)
				.issuedBy(receipt.getIssuedBy().getFullName())
				.issuedDate(receipt.getIssuedDate())
				.status(receipt.getStatus())
				.notes(receipt.getNotes())
				.totalQuantity(details.stream().mapToInt(OutboundReceiptDetail::getQuantity).sum())
				.details(details.stream()
						.map(d -> OutboundReceiptDetailDTO.builder()
								.packageCode(d.getPackageItem().getPackageCode())
								.quantity(d.getQuantity())
								.notes(d.getNotes())
								.build())
						.collect(Collectors.toList()))
				.build();
	}

	@Transactional
	public InboundReceipt createInboundReceipt(InboundReceipt inboundReceipt, List<InboundReceiptDetail> details) {
		InboundReceipt savedReceipt = inboundReceiptRepository.save(inboundReceipt);

		for (InboundReceiptDetail detail : details) {
			detail.setInboundReceipt(savedReceipt);
			inboundReceiptDetailRepository.save(detail);

			updateInventoryOnInbound(savedReceipt.getWarehouse().getId(), detail.getPackageItem().getId(),
					detail.getQuantity());

			if (detail.getWarehouseLocation() != null) {
				WarehouseLocation location = detail.getWarehouseLocation();
				location.setStatus(LocationStatus.OCCUPIED);
				location.setPackageItem(detail.getPackageItem());
				warehouseLocationRepository.save(location);
			}

			// Tạo tracking khi kho nhận hàng
			Package packageItem = detail.getPackageItem();
			Order order = packageItem.getOrder();
			if (order != null) {
				Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElse(null);
				if (shipment != null) {
					String trackingDescription = "Hàng đã đến kho " +
							savedReceipt.getWarehouse().getName();

					// Kiểm tra xem có phải kho đích cuối không
					if (order.getDestinationWarehouse() != null &&
							order.getDestinationWarehouse().getId().equals(savedReceipt.getWarehouse().getId())) {
						trackingDescription += " (Kho đích)";
					} else {
						trackingDescription += " (Kho trung chuyển)";
					}

					trackingService.createTracking(
							shipment,
							0.0,
							0.0,
							trackingDescription,
							TrackingStatus.IN_PROGRESS);
				}
			}

			logWarehouseStatusHistory(savedReceipt.getWarehouse(), detail.getPackageItem(), ChangeType.INBOUND, null,
					"RECEIVED", detail.getQuantity(), savedReceipt.getReceivedBy(),
					"Inbound receipt: " + savedReceipt.getReceiptCode());
		}

		updateWarehouseCurrentStock(savedReceipt.getWarehouse().getId());
		return savedReceipt;
	}

	@Transactional
	public OutboundReceipt createOutboundReceipt(OutboundReceipt outboundReceipt, List<OutboundReceiptDetail> details) {
		OutboundReceipt savedReceipt = outboundReceiptRepository.save(outboundReceipt);

		for (OutboundReceiptDetail detail : details) {
			detail.setOutboundReceipt(savedReceipt);
			outboundReceiptDetailRepository.save(detail);

			updateInventoryOnOutbound(savedReceipt.getWarehouse().getId(), detail.getPackageItem().getId(),
					detail.getQuantity());

			if (detail.getWarehouseLocation() != null) {
				WarehouseLocation location = detail.getWarehouseLocation();
				location.setStatus(LocationStatus.EMPTY);
				location.setPackageItem(null);
				warehouseLocationRepository.save(location);
			}

			Package packageItem = detail.getPackageItem();
			packageItem.setStatus(PackageStatus.DANG_VAN_CHUYEN);
			packageRepository.save(packageItem);

			// Tạo tracking khi kho xuất hàng
			Order order = packageItem.getOrder();
			if (order != null) {
				Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElse(null);
				if (shipment != null) {
					String trackingDescription = "Hàng đã được xuất từ kho " +
							savedReceipt.getWarehouse().getName();

					trackingService.createTracking(
							shipment,
							0.0,
							0.0,
							trackingDescription,
							TrackingStatus.IN_PROGRESS);
				}
			}

			logWarehouseStatusHistory(savedReceipt.getWarehouse(), detail.getPackageItem(), ChangeType.OUTBOUND,
					"IN_WAREHOUSE", "OUT_FOR_DELIVERY", detail.getQuantity(), savedReceipt.getIssuedBy(),
					"Outbound receipt: " + savedReceipt.getReceiptCode());
		}

		updateWarehouseCurrentStock(savedReceipt.getWarehouse().getId());
		return savedReceipt;
	}

	@Transactional
	private void logWarehouseStatusHistory(Warehouse warehouse, Package packageItem, ChangeType changeType,
			String oldStatus, String newStatus, Integer quantity, User user, String notes) {
		WarehouseStatusHistory history = WarehouseStatusHistory.builder().warehouse(warehouse).packageItem(packageItem)
				.changeType(changeType).oldStatus(oldStatus).newStatus(newStatus).quantityChanged(quantity)
				.performedBy(user).notes(notes).build();
		warehouseStatusHistoryRepository.save(history);
	}

	public List<WarehouseLocation> getLocationsByWarehouseId(Long warehouseId) {
		return warehouseLocationRepository.findByWarehouseId(warehouseId);
	}

	public List<WarehouseLocation> getEmptyLocations(Long warehouseId) {
		return warehouseLocationRepository.findByWarehouseIdAndStatus(warehouseId, LocationStatus.EMPTY);
	}

	@Transactional
	public WarehouseLocation createLocation(WarehouseLocation location) {
		if (warehouseLocationRepository.existsByLocationCode(location.getLocationCode())) {
			throw new RuntimeException("Location code already exists");
		}
		return warehouseLocationRepository.save(location);
	}

	@Transactional
	public WarehouseLocation updateLocationStatus(Long locationId, LocationStatus status) {
		WarehouseLocation location = warehouseLocationRepository.findById(locationId)
				.orElseThrow(() -> new RuntimeException("Location not found"));
		location.setStatus(status);
		return warehouseLocationRepository.save(location);
	}

	public List<InboundReceipt> getInboundReceiptsByWarehouseId(Long warehouseId) {
		return inboundReceiptRepository.findByWarehouseId(warehouseId);
	}

	public List<OutboundReceipt> getOutboundReceiptsByWarehouseId(Long warehouseId) {
		return outboundReceiptRepository.findByWarehouseId(warehouseId);
	}

	@Transactional
	public InboundReceipt approveInboundReceipt(Long receiptId) {
		InboundReceipt receipt = inboundReceiptRepository.findById(receiptId)
				.orElseThrow(() -> new RuntimeException("Receipt not found"));
		receipt.setStatus(ReceiptStatus.APPROVED);
		return inboundReceiptRepository.save(receipt);
	}

	@Transactional
	public OutboundReceipt approveOutboundReceipt(Long receiptId) {
		OutboundReceipt receipt = outboundReceiptRepository.findById(receiptId)
				.orElseThrow(() -> new RuntimeException("Receipt not found"));
		receipt.setStatus(ReceiptStatus.APPROVED);
		return outboundReceiptRepository.save(receipt);
	}
}