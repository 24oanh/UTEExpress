package ltweb.controller;

import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.repository.*;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperController {

	private final ShipmentService shipmentService;
	private final OrderService orderService;
	private final AuthService authService;
	private final CloudinaryService cloudinaryService;
	private final NotificationService notificationService;
	private final ShipmentLegRepository shipmentLegRepository;
	private final ShipmentLegService shipmentLegService;
	private final OrderRepository orderRepository;
	private final ShipmentRepository shipmentRepository;
	private final PackageRepository packageRepository;
	private final ShipperRepository shipperRepository;

	@GetMapping("/dashboard")
	public String dashboard(Model model, Authentication auth, HttpSession session) {
		User user = authService.findByUsername(auth.getName());
		Shipper shipper = shipmentService.getShipperByUserId(user.getId());

		session.setAttribute("currentUser", user);
		session.setAttribute("currentShipper", shipper);

		// Lấy đơn hàng được phân công cho shipper này
		List<Order> assignedOrders = orderService.getOrdersByShipperId(shipper.getId());

		// Lấy shipments theo status
		List<Shipment> pendingShipments = shipmentService.getShipmentsByShipperAndStatus(shipper.getId(),
				ShipmentStatus.PENDING);
		List<Shipment> inTransitShipments = shipmentService.getShipmentsByShipperAndStatus(shipper.getId(),
				ShipmentStatus.IN_TRANSIT);

		long unreadNotifications = notificationService.countUnreadNotifications("SHIPPER", shipper.getId());

		model.addAttribute("shipper", shipper);
		model.addAttribute("assignedOrders", assignedOrders);
		model.addAttribute("pendingShipments", pendingShipments);
		model.addAttribute("inTransitShipments", inTransitShipments);
		model.addAttribute("unreadNotifications", unreadNotifications);

		return "shipper/dashboard";
	}

	@GetMapping("/orders")
	public String listOrders(Model model, HttpSession session) {
		Shipper shipper = (Shipper) session.getAttribute("currentShipper");
		List<Order> orders = orderService.getOrdersByShipperId(shipper.getId());
		model.addAttribute("orders", orders);
		return "shipper/orders";
	}

	@GetMapping("/orders/{id}")
	public String orderDetail(@PathVariable Long id, Model model) {
		Order order = orderService.getOrderById(id);
		List<Package> packages = orderService.getPackagesByOrderId(id);
		Shipment shipment = shipmentService.getShipmentByOrderId(id);

		model.addAttribute("order", order);
		model.addAttribute("packages", packages);
		model.addAttribute("shipment", shipment);
		return "shipper/order-detail";
	}

	// ShipperController.java - Sửa method startOrder

	@PostMapping("/orders/{id}/start")
	public String startOrder(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
		try {
			Order order = orderService.getOrderById(id);
			Shipper shipper = (Shipper) session.getAttribute("currentShipper");

			if (!order.getShipper().getId().equals(shipper.getId())) {
				throw new RuntimeException("Bạn không được phân công đơn này");
			}

			if (order.getStatus() != OrderStatus.CHO_GIAO) {
				throw new RuntimeException("Đơn hàng không ở trạng thái chờ giao");
			}

			// Tìm hoặc tạo shipment
			Shipment shipment = shipmentRepository.findByOrderId(id).orElse(null);
			if (shipment == null) {
				shipment = Shipment.builder()
						.shipmentCode("SH" + System.currentTimeMillis())
						.order(order)
						.shipper(shipper)
						.status(ShipmentStatus.PENDING)
						.build();
				shipment = shipmentRepository.save(shipment);

				// Tạo legs nếu chưa có
				shipmentLegService.createShipmentLegs(shipment, order);
			}

			// Bắt đầu shipment
			shipment.setStatus(ShipmentStatus.IN_TRANSIT);
			shipment.setPickupTime(LocalDateTime.now());
			shipmentRepository.save(shipment);

			// Cập nhật order
			order.setStatus(OrderStatus.DANG_GIAO);
			orderRepository.save(order);

			// Bắt đầu chặng đầu tiên
			ShipmentLeg firstLeg = shipmentLegRepository
					.findFirstByShipmentIdAndStatusOrderByLegSequence(shipment.getId(), ShipmentStatus.PENDING)
					.orElse(null);

			if (firstLeg != null) {
				firstLeg.setStatus(ShipmentStatus.IN_TRANSIT);
				firstLeg.setPickupTime(LocalDateTime.now());
				shipmentLegRepository.save(firstLeg);
			}

			redirectAttributes.addFlashAttribute("success", "Đã bắt đầu giao hàng");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
			e.printStackTrace();
		}
		return "redirect:/shipper/orders/" + id;
	}

	@PostMapping("/orders/{id}/complete")
	public String completeOrder(@PathVariable Long id,
			@RequestParam(required = false) MultipartFile proofImage,
			@RequestParam(required = false) String notes,
			RedirectAttributes redirectAttributes,
			HttpSession session) {
		try {
			Order order = orderService.getOrderById(id);
			Shipper shipper = (Shipper) session.getAttribute("currentShipper");

			if (!order.getShipper().getId().equals(shipper.getId())) {
				throw new RuntimeException("Bạn không được phân công đơn này");
			}

			if (order.getStatus() != OrderStatus.DANG_GIAO) {
				throw new RuntimeException("Đơn hàng không ở trạng thái đang giao");
			}

			Shipment shipment = shipmentRepository.findByOrderId(id).orElse(null);
			if (shipment == null) {
				throw new RuntimeException("Không tìm thấy shipment");
			}

			// Upload ảnh chứng từ
			if (proofImage != null && !proofImage.isEmpty()) {
				String imageUrl = cloudinaryService.uploadProofImage(proofImage, shipment.getShipmentCode());
				shipment.setProofImageUrl(imageUrl);
			}

			// Cập nhật ghi chú
			if (notes != null && !notes.isEmpty()) {
				shipment.setNotes(notes);
			}

			// Hoàn thành shipment
			shipment.setStatus(ShipmentStatus.DELIVERED);
			shipment.setDeliveryTime(LocalDateTime.now());
			shipmentRepository.save(shipment);

			// Hoàn thành order
			order.setStatus(OrderStatus.HOAN_THANH);
			orderRepository.save(order);

			// Hoàn thành chặng cuối
			ShipmentLeg lastLeg = shipmentLegRepository
					.findFirstByShipmentIdAndStatusOrderByLegSequence(shipment.getId(), ShipmentStatus.IN_TRANSIT)
					.orElse(null);

			if (lastLeg != null) {
				lastLeg.setStatus(ShipmentStatus.DELIVERED);
				lastLeg.setDeliveryTime(LocalDateTime.now());
				if (proofImage != null && !proofImage.isEmpty()) {
					lastLeg.setNotes("Đã giao thành công");
				}
				shipmentLegRepository.save(lastLeg);
			}

			// Cập nhật packages
			List<Package> packages = packageRepository.findByOrderId(id);
			for (Package pkg : packages) {
				pkg.setStatus(PackageStatus.DA_GIAO);
				packageRepository.save(pkg);
			}

			// Cập nhật thống kê shipper
			shipper.setTotalDeliveries(shipper.getTotalDeliveries() + 1);
			shipper.setSuccessfulDeliveries(shipper.getSuccessfulDeliveries() + 1);
			shipperRepository.save(shipper);

			// Thông báo kho
			notificationService.createNotification("WAREHOUSE", order.getWarehouse().getId(),
					"Đơn hàng " + order.getOrderCode() + " đã giao thành công",
					NotificationType.DELIVERY_COMPLETED, order);

			if (order.getDestinationWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE", order.getDestinationWarehouse().getId(),
						"Đơn hàng " + order.getOrderCode() + " đã được giao đến khách hàng",
						NotificationType.DELIVERY_COMPLETED, order);
			}

			redirectAttributes.addFlashAttribute("success", "Đã hoàn thành giao hàng");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
			e.printStackTrace();
		}
		return "redirect:/shipper/orders/" + id;
	}

	@PostMapping("/orders/{id}/fail")
	public String failOrder(@PathVariable Long id,
			@RequestParam String notes,
			RedirectAttributes redirectAttributes,
			HttpSession session) {
		try {
			if (notes == null || notes.trim().isEmpty()) {
				throw new RuntimeException("Vui lòng nhập lý do thất bại");
			}

			Order order = orderService.getOrderById(id);
			Shipper shipper = (Shipper) session.getAttribute("currentShipper");

			if (!order.getShipper().getId().equals(shipper.getId())) {
				throw new RuntimeException("Bạn không được phân công đơn này");
			}

			Shipment shipment = shipmentRepository.findByOrderId(id).orElse(null);
			if (shipment == null) {
				throw new RuntimeException("Không tìm thấy shipment");
			}

			// Cập nhật shipment
			shipment.setStatus(ShipmentStatus.FAILED);
			shipment.setNotes(notes);
			shipmentRepository.save(shipment);

			// Cập nhật order
			order.setStatus(OrderStatus.THAT_BAI);
			orderRepository.save(order);

			// Cập nhật chặng hiện tại
			ShipmentLeg currentLeg = shipmentLegRepository
					.findFirstByShipmentIdAndStatusOrderByLegSequence(shipment.getId(), ShipmentStatus.IN_TRANSIT)
					.orElse(null);

			if (currentLeg != null) {
				currentLeg.setStatus(ShipmentStatus.FAILED);
				currentLeg.setNotes(notes);
				shipmentLegRepository.save(currentLeg);
			}

			// Cập nhật thống kê shipper
			shipper.setTotalDeliveries(shipper.getTotalDeliveries() + 1);
			shipper.setFailedDeliveries(shipper.getFailedDeliveries() + 1);
			shipperRepository.save(shipper);

			// Thông báo kho
			notificationService.createNotification("WAREHOUSE", order.getWarehouse().getId(),
					"Đơn hàng " + order.getOrderCode() + " giao thất bại: " + notes,
					NotificationType.ORDER_FAILED, order);

			if (order.getDestinationWarehouse() != null) {
				notificationService.createNotification("WAREHOUSE", order.getDestinationWarehouse().getId(),
						"Đơn hàng " + order.getOrderCode() + " giao thất bại",
						NotificationType.ORDER_FAILED, order);
			}

			redirectAttributes.addFlashAttribute("success", "Đã đánh dấu giao hàng thất bại");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
			e.printStackTrace();
		}
		return "redirect:/shipper/orders/" + id;
	}

	@GetMapping("/shipments")
	public String listShipments(Model model, HttpSession session) {
		Shipper shipper = (Shipper) session.getAttribute("currentShipper");
		List<Shipment> shipments = shipmentService.getShipmentsByShipperId(shipper.getId());
		model.addAttribute("shipments", shipments);
		return "shipper/shipments";
	}

	@GetMapping("/shipments/{id}")
	public String shipmentDetail(@PathVariable Long id, Model model) {
		Shipment shipment = shipmentService.getShipmentById(id);
		model.addAttribute("shipment", shipment);
		return "shipper/shipment-detail";
	}

	@PostMapping("/shipments/{id}/start")
	public String startShipment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			shipmentService.updateShipmentStatus(id, ShipmentStatus.IN_TRANSIT);
			redirectAttributes.addFlashAttribute("success", "Shipment started successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to start shipment: " + e.getMessage());
		}
		return "redirect:/shipper/shipments/" + id;
	}

	// ShipperController.java - Thêm method mới
	@GetMapping("/shipments/legs")
	public String listLegs(Model model, HttpSession session) {
		Shipper shipper = (Shipper) session.getAttribute("currentShipper");
		List<ShipmentLeg> pendingLegs = shipmentLegRepository
				.findByShipperIdAndStatus(shipper.getId(), ShipmentStatus.PENDING);
		List<ShipmentLeg> inTransitLegs = shipmentLegRepository
				.findByShipperIdAndStatus(shipper.getId(), ShipmentStatus.IN_TRANSIT);

		model.addAttribute("pendingLegs", pendingLegs);
		model.addAttribute("inTransitLegs", inTransitLegs);
		return "shipper/legs";
	}

	@GetMapping("/shipments/legs/{id}")
	public String legDetail(@PathVariable Long id, Model model) {
		ShipmentLeg leg = shipmentLegRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Leg not found"));
		model.addAttribute("leg", leg);
		return "shipper/leg-detail";
	}

	@PostMapping("/shipments/legs/{id}/start")
	public String startLeg(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			ShipmentLeg leg = shipmentLegRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Leg not found"));
			leg.setStatus(ShipmentStatus.IN_TRANSIT);
			leg.setPickupTime(LocalDateTime.now());
			shipmentLegRepository.save(leg);

			redirectAttributes.addFlashAttribute("success", "Đã bắt đầu chặng giao hàng");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
		}
		return "redirect:/shipper/shipments/legs/" + id;
	}

	@PostMapping("/shipments/legs/{id}/complete")
	public String completeLeg(@PathVariable Long id,
			@RequestParam(required = false) MultipartFile proofImage,
			@RequestParam(required = false) String notes,
			RedirectAttributes redirectAttributes,
			HttpSession session) {
		try {
			ShipmentLeg leg = shipmentLegRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Leg not found"));

			if (proofImage != null && !proofImage.isEmpty()) {
				String imageUrl = cloudinaryService.uploadProofImage(proofImage,
						leg.getShipment().getShipmentCode() + "-leg" + leg.getLegSequence());
				leg.setNotes((leg.getNotes() != null ? leg.getNotes() + "\n" : "") +
						"Ảnh: " + imageUrl);
			}

			if (notes != null && !notes.isEmpty()) {
				leg.setNotes((leg.getNotes() != null ? leg.getNotes() + "\n" : "") + notes);
			}

			shipmentLegRepository.save(leg);
			shipmentLegService.completeCurrentLegAndStartNext(leg.getShipment().getId());

			redirectAttributes.addFlashAttribute("success", "Hoàn thành chặng giao hàng");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
		}
		return "redirect:/shipper/shipments/legs";
	}

	@PostMapping("/shipments/{id}/complete")
	public String completeShipment(@PathVariable Long id, @RequestParam(required = false) MultipartFile proofImage,
			@RequestParam(required = false) String notes, RedirectAttributes redirectAttributes) {
		try {
			if (proofImage != null && !proofImage.isEmpty()) {
				Shipment shipment = shipmentService.getShipmentById(id);
				String imageUrl = cloudinaryService.uploadProofImage(proofImage, shipment.getShipmentCode());
				shipmentService.uploadProofImage(id, imageUrl);
			}

			if (notes != null && !notes.isEmpty()) {
				Shipment shipment = shipmentService.getShipmentById(id);
				shipment.setNotes(notes);
				shipmentService.updateShipment(id, shipment);
			}

			shipmentService.updateShipmentStatus(id, ShipmentStatus.DELIVERED);
			redirectAttributes.addFlashAttribute("success", "Hoàn thành chặng giao hàng");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
		}
		return "redirect:/shipper/shipments/" + id;
	}

	@PostMapping("/shipments/{id}/fail")
	public String failShipment(@PathVariable Long id, @RequestParam String notes,
			RedirectAttributes redirectAttributes) {
		try {
			Shipment shipment = shipmentService.getShipmentById(id);
			shipment.setNotes(notes);
			shipmentService.updateShipment(id, shipment);
			shipmentService.updateShipmentStatus(id, ShipmentStatus.FAILED);
			redirectAttributes.addFlashAttribute("success", "Shipment marked as failed");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to mark shipment as failed: " + e.getMessage());
		}
		return "redirect:/shipper/shipments/" + id;
	}

	@PostMapping("/shipments/{id}/upload-proof")
	public String uploadProof(@PathVariable Long id, @RequestParam MultipartFile proofImage,
			RedirectAttributes redirectAttributes) {
		try {
			Shipment shipment = shipmentService.getShipmentById(id);
			String imageUrl = cloudinaryService.uploadProofImage(proofImage, shipment.getShipmentCode());
			shipmentService.uploadProofImage(id, imageUrl);
			redirectAttributes.addFlashAttribute("success", "Proof image uploaded successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to upload proof image: " + e.getMessage());
		}
		return "redirect:/shipper/shipments/" + id;
	}

	@GetMapping("/profile")
	public String profile(Model model, HttpSession session) {
		Shipper shipper = (Shipper) session.getAttribute("currentShipper");
		model.addAttribute("shipper", shipper);
		return "shipper/profile";
	}

	@PostMapping("/location/update")
	@ResponseBody
	public String updateLocation(@RequestParam Double latitude, @RequestParam Double longitude, HttpSession session) {
		try {
			Shipper shipper = (Shipper) session.getAttribute("currentShipper");
			shipmentService.updateShipperLocation(shipper.getId(), latitude, longitude);
			return "success";
		} catch (Exception e) {
			return "error";
		}
	}
}