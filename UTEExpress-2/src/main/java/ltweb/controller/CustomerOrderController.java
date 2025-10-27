package ltweb.controller;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;
import ltweb.service.TrackingService;

@Controller
@RequestMapping("/customer/orders")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerOrderController {

	private final CustomerOrderService customerOrderService;
	private final WarehouseService warehouseService;
	private final OrderService orderService;
	private final TrackingService trackingService;


	@GetMapping
	public String listOrders(Model model, HttpSession session) {
	    Customer customer = (Customer) session.getAttribute("currentCustomer");
	    List<Order> orders = customerOrderService.getCustomerOrders(customer.getId());
	    model.addAttribute("orders", orders);
	    return "customer/orders";
	}

	@GetMapping("/create")
	public String createOrderForm(Model model) {
		CreateOrderDTO dto = new CreateOrderDTO();
		model.addAttribute("orderDTO", dto);
		model.addAttribute("warehouses", warehouseService.getAllWarehouses());
		model.addAttribute("serviceTypes", ServiceType.values());
		return "customer/create-order";
	}

	@PostMapping("/calculate")
	public String calculateOrder(@Valid @ModelAttribute("orderDTO") CreateOrderDTO dto, BindingResult result,
			Model model, HttpSession session) {

		System.out.println("=== DEBUG CALCULATE ORDER ===");
		System.out.println("DTO: " + dto);
		System.out.println("Has errors: " + result.hasErrors());

		if (result.hasErrors()) {
			System.out.println("Validation errors:");
			result.getAllErrors().forEach(error -> {
				System.out.println("  - " + error.getDefaultMessage());
			});
			model.addAttribute("warehouses", warehouseService.getAllWarehouses());
			model.addAttribute("serviceTypes", ServiceType.values());
			return "customer/create-order";
		}

		try {
			System.out.println("Calculating order summary...");
			OrderSummaryDTO summary = customerOrderService.calculateOrderSummary(dto);
			System.out.println("Summary: " + summary);

			session.setAttribute("orderDTO", dto);
			session.setAttribute("orderSummary", summary);
			model.addAttribute("summary", summary);

			System.out.println("Redirecting to confirm page");
			return "customer/order-confirm";
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();

			model.addAttribute("error", e.getMessage());
			model.addAttribute("warehouses", warehouseService.getAllWarehouses());
			model.addAttribute("serviceTypes", ServiceType.values());
			return "customer/create-order";
		}
	}

	@GetMapping("/confirm")
	public String confirmOrder(Model model, HttpSession session) {
		CreateOrderDTO orderDTO = (CreateOrderDTO) session.getAttribute("orderDTO");
		OrderSummaryDTO summary = (OrderSummaryDTO) session.getAttribute("orderSummary");

		if (orderDTO == null || summary == null) {
			return "redirect:/customer/orders/create";
		}

		model.addAttribute("orderDTO", orderDTO);
		model.addAttribute("summary", summary);
		return "customer/order-confirm";
	}

	@PostMapping("/submit")
	public String submitOrder(HttpSession session, RedirectAttributes redirectAttributes) {
	    CreateOrderDTO orderDTO = (CreateOrderDTO) session.getAttribute("orderDTO");
	    Customer customer = (Customer) session.getAttribute("currentCustomer");

	    if (orderDTO == null) {
	        redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin đơn hàng");
	        return "redirect:/customer/orders/create";
	    }

	    try {
	        Order order = customerOrderService.createOrder(orderDTO, customer.getId());
	        
	        session.removeAttribute("orderDTO");
	        session.removeAttribute("orderSummary");

	        return "redirect:/customer/payment/" + order.getId();
	        
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("error", "Lỗi tạo đơn hàng: " + e.getMessage());
	        return "redirect:/customer/orders/confirm";
	    }
	}
	@GetMapping("/{id}")
	public String orderDetail(@PathVariable Long id, Model model, HttpSession session) {
		Customer customer = (Customer) session.getAttribute("currentCustomer");
		Order order = customerOrderService.getOrderById(id);

		if (!order.getCustomer().getId().equals(customer.getId())) {
			return "redirect:/customer/orders";
		}

		List<Package> packages = orderService.getPackagesByOrderId(id);
		model.addAttribute("order", order);
		model.addAttribute("packages", packages);
		return "customer/order-detail";
	}

	@PostMapping("/{id}/cancel")
	public String cancelOrder(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		Customer customer = (Customer) session.getAttribute("currentCustomer");
		Order order = customerOrderService.getOrderById(id);

		if (!order.getCustomer().getId().equals(customer.getId())) {
			return "redirect:/customer/orders";
		}

		if (order.getStatus() != OrderStatus.CHO_GIAO) {
			redirectAttributes.addFlashAttribute("error", "Không thể hủy đơn hàng ở trạng thái hiện tại");
			return "redirect:/customer/orders/" + id;
		}

		try {
			orderService.cancelOrder(id);
			redirectAttributes.addFlashAttribute("success", "Hủy đơn hàng thành công");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
		}

		return "redirect:/customer/orders/" + id;
	}
	
	@GetMapping("/history")
	public String orderHistory(Model model, HttpSession session,
	        @RequestParam(required = false) String startDate,
	        @RequestParam(required = false) String endDate,
	        @RequestParam(required = false) String status,
	        @RequestParam(required = false) String orderCode) {
	    Customer customer = (Customer) session.getAttribute("currentCustomer");
	    List<Order> orders = customerOrderService.getCustomerOrders(customer.getId());
	    
	    // Lọc theo ngày
	    if (startDate != null && !startDate.isEmpty()) {
	        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
	        orders = orders.stream()
	            .filter(o -> o.getCreatedAt().isAfter(start))
	            .collect(Collectors.toList());
	    }
	    
	    if (endDate != null && !endDate.isEmpty()) {
	        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
	        orders = orders.stream()
	            .filter(o -> o.getCreatedAt().isBefore(end))
	            .collect(Collectors.toList());
	    }
	    
	    // Lọc theo trạng thái
	    if (status != null && !status.isEmpty()) {
	        OrderStatus orderStatus = OrderStatus.valueOf(status);
	        orders = orders.stream()
	            .filter(o -> o.getStatus() == orderStatus)
	            .collect(Collectors.toList());
	    }
	    
	    // Lọc theo mã vận đơn
	    if (orderCode != null && !orderCode.isEmpty()) {
	        orders = orders.stream()
	            .filter(o -> o.getOrderCode().toLowerCase().contains(orderCode.toLowerCase()))
	            .collect(Collectors.toList());
	    }
	    
	    // Sắp xếp theo ngày tạo mới nhất
	    orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
	    
	    model.addAttribute("orders", orders);
	    return "customer/order-history";
	}
	
	@GetMapping("/{id}/detail-ajax")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getOrderDetailAjax(@PathVariable Long id, HttpSession session) {
	    Customer customer = (Customer) session.getAttribute("currentCustomer");
	    Order order = customerOrderService.getOrderById(id);
	    
	    if (!order.getCustomer().getId().equals(customer.getId())) {
	        return ResponseEntity.status(403).build();
	    }
	    
	    List<Package> packages = orderService.getPackagesByOrderId(id);
	    Shipment shipment = orderService.getShipmentByOrderId(id);
	    List<Tracking> trackings = shipment != null ? 
	        trackingService.getTrackingsByShipmentId(shipment.getId()) : new ArrayList<>();
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("id", order.getId());
	    response.put("orderCode", order.getOrderCode());
	    response.put("status", order.getStatus().name());
	    response.put("senderName", order.getSenderName());
	    response.put("senderPhone", order.getSenderPhone());
	    response.put("senderAddress", order.getSenderAddress());
	    response.put("recipientName", order.getRecipientName());
	    response.put("recipientPhone", order.getRecipientPhone());
	    response.put("recipientAddress", order.getRecipientAddress());
	    response.put("shipmentFee", order.getShipmentFee());
	    response.put("createdAt", order.getCreatedAt());
	    response.put("serviceTypeName", getServiceTypeName(order.getServiceType()));
	    
	    List<Map<String, Object>> packageList = packages.stream().map(pkg -> {
	        Map<String, Object> pkgMap = new HashMap<>();
	        pkgMap.put("description", pkg.getDescription());
	        pkgMap.put("weight", pkg.getWeight());
	        pkgMap.put("length", pkg.getLength());
	        pkgMap.put("width", pkg.getWidth());
	        pkgMap.put("height", pkg.getHeight());
	        pkgMap.put("unitQuantity", pkg.getUnitQuantity());
	        return pkgMap;
	    }).collect(Collectors.toList());
	    response.put("packages", packageList);
	    
	    List<Map<String, Object>> trackingList = trackings.stream().map(t -> {
	        Map<String, Object> trackMap = new HashMap<>();
	        trackMap.put("description", t.getDescription());
	        trackMap.put("status", t.getStatus().name());
	        trackMap.put("createdAt", t.getCreatedAt());
	        return trackMap;
	    }).collect(Collectors.toList());
	    response.put("trackings", trackingList);
	    
	    return ResponseEntity.ok(response);
	}

	
	@GetMapping("/tracking")
	public String trackingPage(Model model) {
	    return "customer/tracking";
	}
	
	private String getServiceTypeName(ServiceType serviceType) {
	    switch (serviceType) {
	        case EXPRESS: return "Nhanh";
	        case ECONOMY: return "Tiết kiệm";
	        case STANDARD: 
	        default: return "Chuẩn";
	    }
	}
}