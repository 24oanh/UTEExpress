package ltweb.controller;

import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.service.OrderService;
import ltweb.service.ShipmentLegService;
import ltweb.service.ShipmentService;
import ltweb.service.WarehouseService;
import ltweb.repository.ShipmentLegRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/warehouse/orders")
@PreAuthorize("hasRole('WAREHOUSE_STAFF')")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;
	private final ShipmentService shipmentService;
	private final WarehouseService warehouseService;
	private final ShipmentLegRepository shipmentLegRepository;
	private final ShipmentLegService shipmentLegService;

	@GetMapping
	public String listOrders(Model model, HttpSession session) {
		Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
		List<Order> orders = orderService.getOrdersByWarehouseId(warehouse.getId());
		model.addAttribute("orders", orders);
		return "warehouse/orders";
	}

	@GetMapping("/{id}")
	public String orderDetail(@PathVariable Long id, Model model) {
		Order order = orderService.getOrderById(id);
		List<Package> packages = orderService.getPackagesByOrderId(id);
		List<Shipper> availableShippers = shipmentService.getActiveShippers();

		Shipment shipment = shipmentService.getShipmentByOrderId(id);
		if (shipment != null) {
			List<ShipmentLeg> legs = shipmentLegRepository.findByShipmentIdOrderByLegSequence(shipment.getId());
			model.addAttribute("shipmentLegs", legs);
		}

		model.addAttribute("order", order);
		model.addAttribute("packages", packages);
		model.addAttribute("availableShippers", availableShippers);
		return "warehouse/order-detail";
	}

	@GetMapping("/create")
	public String createOrderForm(Model model, HttpSession session) {
		Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
		List<Warehouse> allWarehouses = warehouseService.getAllWarehouses();

		model.addAttribute("order", new Order());
		model.addAttribute("currentWarehouse", warehouse);
		model.addAttribute("allWarehouses", allWarehouses);
		return "warehouse/order-form";
	}

	@PostMapping("/create")
	public String createOrder(@ModelAttribute Order order, @RequestParam Long destinationWarehouseId,
			HttpSession session, RedirectAttributes redirectAttributes) {
		try {
			Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
			Warehouse destinationWarehouse = warehouseService.getWarehouseById(destinationWarehouseId);

			order.setWarehouse(warehouse);
			order.setDestinationWarehouse(destinationWarehouse);
			orderService.createOrder(order);

			redirectAttributes.addFlashAttribute("success", "Order created successfully with route legs");
			return "redirect:/warehouse/orders";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to create order: " + e.getMessage());
			return "redirect:/warehouse/orders/create";
		}
	}

	@GetMapping("/{id}/edit")
	public String editOrderForm(@PathVariable Long id, Model model) {
		Order order = orderService.getOrderById(id);
		model.addAttribute("order", order);
		return "warehouse/order-form";
	}

	@PostMapping("/{id}/update")
	public String updateOrder(@PathVariable Long id, @ModelAttribute Order orderDetails,
			RedirectAttributes redirectAttributes) {
		try {
			orderService.updateOrder(id, orderDetails);
			redirectAttributes.addFlashAttribute("success", "Order updated successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to update order: " + e.getMessage());
		}
		return "redirect:/warehouse/orders/" + id;
	}

	@PostMapping("/{id}/assign-shipper")
	public String assignShipper(@PathVariable Long id, @RequestParam Long shipperId,
			RedirectAttributes redirectAttributes) {
		try {
			orderService.assignOrderToShipper(id, shipperId);
			redirectAttributes.addFlashAttribute("success", "Shipper assigned and route updated successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to assign shipper: " + e.getMessage());
		}
		return "redirect:/warehouse/orders/" + id;
	}

	// OrderController.java - Thêm method reassign leg
	@PostMapping("/orders/{orderId}/legs/{legId}/reassign")
	public String reassignLeg(@PathVariable Long orderId, @PathVariable Long legId,
			@RequestParam Long shipperId, RedirectAttributes redirectAttributes) {
		try {
			shipmentLegService.reassignLeg(legId, shipperId);
			redirectAttributes.addFlashAttribute("success", "Đã phân công lại shipper");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
		}
		return "redirect:/warehouse/orders/" + orderId;
	}

	@PostMapping("/{id}/update-status")
	public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status,
			RedirectAttributes redirectAttributes) {
		try {
			orderService.updateOrderStatus(id, status);
			redirectAttributes.addFlashAttribute("success", "Order status updated successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to update order status: " + e.getMessage());
		}
		return "redirect:/warehouse/orders/" + id;
	}

	@PostMapping("/{id}/cancel")
	public String cancelOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			orderService.cancelOrder(id);
			redirectAttributes.addFlashAttribute("success", "Order cancelled successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to cancel order: " + e.getMessage());
		}
		return "redirect:/warehouse/orders/" + id;
	}

	@GetMapping("/{id}/packages/add")
	public String addPackageForm(@PathVariable Long id, Model model) {
		Order order = orderService.getOrderById(id);
		model.addAttribute("order", order);
		model.addAttribute("package", new Package());
		return "warehouse/package-form";
	}

	@PostMapping("/{id}/packages/add")
	public String addPackage(@PathVariable Long id, @ModelAttribute Package packageItem,
			RedirectAttributes redirectAttributes) {
		try {
			orderService.addPackageToOrder(id, packageItem);
			redirectAttributes.addFlashAttribute("success", "Package added successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to add package: " + e.getMessage());
		}
		return "redirect:/warehouse/orders/" + id;
	}

	@GetMapping("/shippers")
	public String listShippers(Model model) {
		List<Shipper> shippers = shipmentService.getActiveShippers();
		model.addAttribute("shippers", shippers);
		return "warehouse/shippers";
	}
}