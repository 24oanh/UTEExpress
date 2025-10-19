package ltweb.controller;

import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.service.AuthService;
import ltweb.service.OrderService;
import ltweb.service.ShipmentService;
import ltweb.service.CloudinaryService;
import ltweb.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
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
			redirectAttributes.addFlashAttribute("success", "Shipment completed successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to complete shipment: " + e.getMessage());
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