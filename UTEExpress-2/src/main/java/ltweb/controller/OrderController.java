// ltweb1/controller/OrderController.java - XÓA create và edit methods
package ltweb.controller;

import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.service.NotificationService;
import ltweb.service.OrderService;
import ltweb.service.RouteCalculationService;
import ltweb.service.ShipmentLegService;
import ltweb.service.ShipmentService;
import ltweb.service.WarehouseService;
import ltweb.repository.OrderRepository;
import ltweb.repository.ShipmentLegRepository;
import ltweb.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/warehouse/orders")
@PreAuthorize("hasRole('WAREHOUSE_STAFF')")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ShipmentService shipmentService;
    private final WarehouseService warehouseService;
    private final ShipmentLegRepository shipmentLegRepository;
    private final OrderRepository orderRepository;
    private final ShipmentLegService shipmentLegService;
    private final ShipmentRepository shipmentRepository;
    private final NotificationService notificationService;

    @GetMapping
    public String listOrders(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<Order> orders = orderService.getOrdersByWarehouseId(warehouse.getId());
        model.addAttribute("orders", orders);
        return "warehouse/orders";
    }

    // Thêm vào /warehouse/orders/{id} detail page
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model, HttpSession session) {
        Order order = orderService.getOrderById(id);
        List<Package> packages = orderService.getPackagesByOrderId(id);
        List<Shipper> availableShippers = shipmentService.getActiveShippers();
        Shipment shipment = shipmentService.getShipmentByOrderId(id);
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");

        // Kiểm tra xem đã có inbound receipt chưa
        List<InboundReceipt> inboundReceipts = warehouseService.getInboundReceiptsByOrderId(id);
        boolean hasInboundReceipt = !inboundReceipts.isEmpty();

        // Kiểm tra xem đã có outbound receipt chưa
        List<OutboundReceipt> outboundReceipts = warehouseService.getOutboundReceiptsByOrderId(id);
        boolean hasOutboundReceipt = !outboundReceipts.isEmpty();

        // Kiểm tra tồn kho
        List<Inventory> inventories = packages.stream()
                .map(pkg -> warehouseService.getInventoryByPackageId(warehouse.getId(), pkg.getId()))
                .filter(inv -> inv != null)
                .collect(Collectors.toList());

        if (shipment != null) {
            List<ShipmentLeg> legs = shipmentLegRepository.findByShipmentIdOrderByLegSequence(shipment.getId());
            model.addAttribute("shipmentLegs", legs);
        }

        model.addAttribute("order", order);
        model.addAttribute("packages", packages);
        model.addAttribute("availableShippers", availableShippers);
        model.addAttribute("hasInboundReceipt", hasInboundReceipt);
        model.addAttribute("hasOutboundReceipt", hasOutboundReceipt);
        model.addAttribute("inventories", inventories);
        model.addAttribute("inboundReceipts", inboundReceipts);
        model.addAttribute("outboundReceipts", outboundReceipts);

        return "warehouse/order-detail";
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

    @PostMapping("/{id}/create-shipment")
    public String createShipmentForOrder(@PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        try {
            Order order = orderService.getOrderById(id);
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");

            if (!order.getWarehouse().getId().equals(warehouse.getId())) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền tạo shipment cho đơn này");
                return "redirect:/warehouse/orders/" + id;
            }

            Shipment existingShipment = shipmentService.getShipmentByOrderId(id);
            if (existingShipment != null) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng đã có shipment");
                return "redirect:/warehouse/orders/" + id;
            }

            RouteCalculationService.RouteSegment firstSegment = shipmentLegService.getFirstLegPreferredShipper(order);

            Shipper defaultShipper = null;
            if (firstSegment != null && firstSegment.getPreferredShipper() != null) {
                defaultShipper = firstSegment.getPreferredShipper();
            }

            Shipment shipment = Shipment.builder()
                    .shipmentCode("SH" + System.currentTimeMillis())
                    .order(order)
                    .shipper(defaultShipper)
                    .status(ShipmentStatus.PENDING)
                    .build();

            shipment = shipmentRepository.save(shipment);
            shipmentLegService.createShipmentLegs(shipment, order);

            if (defaultShipper != null) {
                order.setShipper(defaultShipper);
                orderRepository.save(order);

                notificationService.createNotification(
                        defaultShipper.getUser().getId(),
                        "SHIPPER",
                        "Bạn được phân công đơn hàng: " + order.getOrderCode(),
                        NotificationType.ORDER_ASSIGNED,
                        order);
            }

            redirectAttributes.addFlashAttribute("success", "✓ Đã tạo shipment thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo shipment: " + e.getMessage());
        }

        return "redirect:/warehouse/orders/" + id;
    }
}