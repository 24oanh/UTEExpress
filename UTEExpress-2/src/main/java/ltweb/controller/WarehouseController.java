// ltweb1/controller/WarehouseController.java
package ltweb.controller;

import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/warehouse")
@PreAuthorize("hasRole('WAREHOUSE_STAFF')")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final AuthService authService;
    private final ShipmentService shipmentService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth, HttpSession session) {
        User user = authService.findByUsername(auth.getName());
        Warehouse warehouse = warehouseService.getWarehouseByUserId(user.getId());

        session.setAttribute("currentUser", user);
        session.setAttribute("currentWarehouse", warehouse);

        List<Order> pendingOrders = orderService.getOrdersByWarehouseAndStatus(warehouse.getId(), OrderStatus.CHO_GIAO);
        List<Order> inProgressOrders = orderService.getOrdersByWarehouseAndStatus(warehouse.getId(), OrderStatus.DANG_GIAO);
        List<Inventory> inventories = warehouseService.getInventoryByWarehouseId(warehouse.getId());

        long unreadNotifications = notificationService.countUnreadNotifications(user.getId());

        long totalOrders = orderService.getOrdersByWarehouseId(warehouse.getId()).size();
        long completedOrders = orderService.countOrdersByWarehouseAndStatus(warehouse.getId(), OrderStatus.HOAN_THANH);

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("inProgressOrders", inProgressOrders);
        model.addAttribute("inventories", inventories);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("completedOrders", completedOrders);

        return "warehouse/dashboard";
    }

    @GetMapping("/info")
    public String warehouseInfo(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        model.addAttribute("warehouse", warehouse);
        return "warehouse/info";
    }

    @PostMapping("/info/update")
    public String updateWarehouseInfo(@ModelAttribute Warehouse warehouseDetails,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            warehouseService.updateWarehouse(warehouse.getId(), warehouseDetails);
            redirectAttributes.addFlashAttribute("success", "Warehouse information updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update warehouse information: " + e.getMessage());
        }

        return "redirect:/warehouse/info";
    }

    @GetMapping("/inventory")
    public String inventory(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<Inventory> inventories = warehouseService.getInventoryByWarehouseId(warehouse.getId());

        model.addAttribute("inventories", inventories);
        model.addAttribute("warehouse", warehouse);

        return "warehouse/inventory";
    }

    @GetMapping("/locations")
    public String locations(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<WarehouseLocation> locations = warehouseService.getLocationsByWarehouseId(warehouse.getId());

        model.addAttribute("locations", locations);
        model.addAttribute("warehouse", warehouse);

        return "warehouse/locations";
    }

    @GetMapping("/locations/create")
    public String createLocationForm(Model model) {
        model.addAttribute("location", new WarehouseLocation());
        return "warehouse/location-form";
    }

    @PostMapping("/locations/create")
    public String createLocation(@ModelAttribute WarehouseLocation location,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            location.setWarehouse(warehouse);
            warehouseService.createLocation(location);
            redirectAttributes.addFlashAttribute("success", "Location created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create location: " + e.getMessage());
        }

        return "redirect:/warehouse/locations";
    }

    @PostMapping("/locations/{id}/update-status")
    public String updateLocationStatus(@PathVariable Long id,
                                      @RequestParam LocationStatus status,
                                      RedirectAttributes redirectAttributes) {
        try {
            warehouseService.updateLocationStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Location status updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update location status: " + e.getMessage());
        }

        return "redirect:/warehouse/locations";
    }

    @GetMapping("/inbound")
    public String inboundReceipts(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<InboundReceipt> receipts = warehouseService.getInboundReceiptsByWarehouseId(warehouse.getId());

        model.addAttribute("receipts", receipts);

        return "warehouse/inbound-receipts";
    }

    @GetMapping("/inbound/create")
    public String createInboundForm(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<Order> orders = orderService.getOrdersByWarehouseId(warehouse.getId());
        List<WarehouseLocation> emptyLocations = warehouseService.getEmptyLocations(warehouse.getId());

        model.addAttribute("receipt", new InboundReceipt());
        model.addAttribute("orders", orders);
        model.addAttribute("emptyLocations", emptyLocations);

        return "warehouse/inbound-form";
    }

    @PostMapping("/inbound/create")
    public String createInbound(@ModelAttribute InboundReceipt receipt,
                               @RequestParam List<Long> packageIds,
                               @RequestParam List<Integer> quantities,
                               @RequestParam(required = false) List<Long> locationIds,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");

            receipt.setWarehouse(warehouse);
            receipt.setReceivedBy(user);

            List<InboundReceiptDetail> details = new java.util.ArrayList<>();

            for (int i = 0; i < packageIds.size(); i++) {
                InboundReceiptDetail detail = InboundReceiptDetail.builder()
                        .packageItem(Package.builder().id(packageIds.get(i)).build())
                        .quantity(quantities.get(i))
                        .warehouseLocation(locationIds != null && i < locationIds.size() && locationIds.get(i) != null
                                ? WarehouseLocation.builder().id(locationIds.get(i)).build()
                                : null)
                        .build();

                details.add(detail);
            }

            warehouseService.createInboundReceipt(receipt, details);

            redirectAttributes.addFlashAttribute("success", "Inbound receipt created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create inbound receipt: " + e.getMessage());
        }

        return "redirect:/warehouse/inbound";
    }

    @PostMapping("/inbound/{id}/approve")
    public String approveInbound(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            warehouseService.approveInboundReceipt(id);
            redirectAttributes.addFlashAttribute("success", "Inbound receipt approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve inbound receipt: " + e.getMessage());
        }

        return "redirect:/warehouse/inbound";
    }

    @GetMapping("/outbound")
    public String outboundReceipts(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<OutboundReceipt> receipts = warehouseService.getOutboundReceiptsByWarehouseId(warehouse.getId());

        model.addAttribute("receipts", receipts);

        return "warehouse/outbound-receipts";
    }

    @GetMapping("/outbound/create")
    public String createOutboundForm(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<Order> orders = orderService.getOrdersByWarehouseId(warehouse.getId());
        List<Inventory> availableInventory = warehouseService.getAvailableInventory(warehouse.getId());
        List<Shipper> shippers = shipmentService.getActiveShippers();

        model.addAttribute("receipt", new OutboundReceipt());
        model.addAttribute("orders", orders);
        model.addAttribute("availableInventory", availableInventory);
        model.addAttribute("shippers", shippers);

        return "warehouse/outbound-form";
    }

    @PostMapping("/outbound/create")
    public String createOutbound(@ModelAttribute OutboundReceipt receipt,
                                @RequestParam List<Long> packageIds,
                                @RequestParam List<Integer> quantities,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");

            receipt.setWarehouse(warehouse);
            receipt.setIssuedBy(user);

            List<OutboundReceiptDetail> details = new java.util.ArrayList<>();

            for (int i = 0; i < packageIds.size(); i++) {
                OutboundReceiptDetail detail = OutboundReceiptDetail.builder()
                        .packageItem(Package.builder().id(packageIds.get(i)).build())
                        .quantity(quantities.get(i))
                        .build();

                details.add(detail);
            }

            warehouseService.createOutboundReceipt(receipt, details);

            redirectAttributes.addFlashAttribute("success", "Outbound receipt created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create outbound receipt: " + e.getMessage());
        }

        return "redirect:/warehouse/outbound";
    }

    @PostMapping("/outbound/{id}/approve")
    public String approveOutbound(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            warehouseService.approveOutboundReceipt(id);
            redirectAttributes.addFlashAttribute("success", "Outbound receipt approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve outbound receipt: " + e.getMessage());
        }

        return "redirect:/warehouse/outbound";
    }
}