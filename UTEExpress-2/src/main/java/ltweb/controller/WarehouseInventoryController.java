package ltweb.controller;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class WarehouseInventoryController {

    private final WarehouseService warehouseService;

    // ==================== NHẬP KHO ====================

    @GetMapping("/inbound/receive-order/{orderId}")
    public String receiveOrderForm(@PathVariable Long orderId, Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");

        model.addAttribute("orderId", orderId);
        model.addAttribute("warehouse", warehouse);

        return "warehouse/receive-order-form";
    }

    @PostMapping("/inbound/receive-order/{orderId}")
    public String receiveOrder(@PathVariable Long orderId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");

            InboundReceipt receipt = warehouseService.receiveOrderToWarehouse(
                    orderId, warehouse.getId(), user);

            redirectAttributes.addFlashAttribute("success",
                    "✓ Nhập kho thành công! Mã phiếu: " + receipt.getReceiptCode());

            return "redirect:/warehouse/inbound";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi nhập kho: " + e.getMessage());
            return "redirect:/warehouse/orders/" + orderId;
        }
    }

    // ==================== XUẤT KHO ====================

    @GetMapping("/outbound/issue-order/{orderId}")
    public String issueOrderForm(@PathVariable Long orderId, Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");

        model.addAttribute("orderId", orderId);
        model.addAttribute("warehouse", warehouse);

        return "warehouse/issue-order-form";
    }

    @PostMapping("/outbound/issue-order/{orderId}")
    public String issueOrder(@PathVariable Long orderId,
            @RequestParam Long shipperId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("currentUser");

            OutboundReceipt receipt = warehouseService.issueOrderToShipper(
                    orderId, shipperId, user);

            redirectAttributes.addFlashAttribute("success",
                    "✓ Xuất kho thành công! Mã phiếu: " + receipt.getReceiptCode());

            return "redirect:/warehouse/outbound";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xuất kho: " + e.getMessage());
            return "redirect:/warehouse/orders/" + orderId;
        }
    }

    // ==================== TỒN KHO ====================

    @GetMapping("/inventory")
    public String inventoryPage(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");

        List<Inventory> inventories = warehouseService.getInventoryByWarehouseId(warehouse.getId());

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("inventories", inventories);

        return "warehouse/inventory";
    }

    @GetMapping("/inventory/report")
    public String inventoryReportPage(Model model, HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        InventoryReportDTO report = warehouseService.getInventoryReport(
                warehouse.getId(), startDate, endDate);

        model.addAttribute("report", report);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "warehouse/inventory-report";
    }

    // ==================== BÁO CÁO NHẬP XUẤT ====================

    @GetMapping("/reports/inbound-outbound")
    public String inboundOutboundReportPage(Model model, HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        InboundOutboundReportDTO report = warehouseService.getInboundOutboundReport(
                warehouse.getId(), startDate, endDate);

        model.addAttribute("report", report);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "warehouse/inbound-outbound-report";
    }

    // ==================== API ====================

    @GetMapping("/api/inventory/{warehouseId}")
    @ResponseBody
    public ResponseEntity<List<Inventory>> getInventory(@PathVariable Long warehouseId) {
        List<Inventory> inventories = warehouseService.getInventoryByWarehouseId(warehouseId);
        return ResponseEntity.ok(inventories);
    }

    @GetMapping("/api/inventory/report/{warehouseId}")
    @ResponseBody
    public ResponseEntity<InventoryReportDTO> getInventoryReportAPI(
            @PathVariable Long warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        InventoryReportDTO report = warehouseService.getInventoryReport(warehouseId, startDate, endDate);
        return ResponseEntity.ok(report);
    }
}