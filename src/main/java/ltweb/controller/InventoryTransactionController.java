package ltweb.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import ltweb.entity.InventoryTransaction;
import ltweb.entity.LocationStatus;
import ltweb.entity.User;
import ltweb.entity.Warehouse;
import ltweb.entity.WarehouseLocation;
import ltweb.repository.PackageRepository;
import ltweb.repository.WarehouseLocationRepository;
import ltweb.service.InventoryTransactionService;
import ltweb.entity.Package;

@Controller
@RequestMapping("/warehouse/inventory/transactions")
@PreAuthorize("hasRole('WAREHOUSE_STAFF')")
@RequiredArgsConstructor
public class InventoryTransactionController {
    private final InventoryTransactionService transactionService;
    private final PackageRepository packageRepository;
    private final WarehouseLocationRepository locationRepository;

    @GetMapping
    public String listTransactions(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<InventoryTransaction> transactions = transactionService.getTransactionsByWarehouse(warehouse.getId());
        model.addAttribute("transactions", transactions);
        return "warehouse/inventory-transactions";
    }

    @GetMapping("/record-inbound")
    public String recordInboundForm(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<Package> packages = packageRepository.findAll();
        List<WarehouseLocation> emptyLocations = locationRepository.findByWarehouseIdAndStatus(
            warehouse.getId(), LocationStatus.EMPTY);
        
        model.addAttribute("packages", packages);
        model.addAttribute("emptyLocations", emptyLocations);
        return "warehouse/record-inbound";
    }

    @PostMapping("/record-inbound")
    public String recordInbound(@RequestParam Long packageId,
                               @RequestParam Integer quantity,
                               @RequestParam Long toLocationId,
                               @RequestParam(required = false) String notes,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");
            
            transactionService.recordInbound(warehouse.getId(), packageId, quantity, 
                                           toLocationId, user, notes);
            redirectAttributes.addFlashAttribute("success", "Ghi nhận nhập kho thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/warehouse/inventory/transactions";
    }

    @GetMapping("/transfer-location")
    public String transferLocationForm(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<WarehouseLocation> occupiedLocations = locationRepository.findByWarehouseIdAndStatus(
            warehouse.getId(), LocationStatus.OCCUPIED);
        List<WarehouseLocation> emptyLocations = locationRepository.findByWarehouseIdAndStatus(
            warehouse.getId(), LocationStatus.EMPTY);
        
        model.addAttribute("occupiedLocations", occupiedLocations);
        model.addAttribute("emptyLocations", emptyLocations);
        return "warehouse/transfer-location";
    }

    @PostMapping("/transfer-location")
    public String transferLocation(@RequestParam Long packageId,
                                  @RequestParam Long fromLocationId,
                                  @RequestParam Long toLocationId,
                                  @RequestParam(required = false) String notes,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("currentUser");
            transactionService.transferLocation(packageId, fromLocationId, toLocationId, user, notes);
            redirectAttributes.addFlashAttribute("success", "Chuyển vị trí thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/warehouse/inventory/transactions";
    }

    @GetMapping("/package/{packageId}")
    public String packageTransactionHistory(@PathVariable Long packageId, Model model) {
        List<InventoryTransaction> transactions = transactionService.getTransactionsByPackage(packageId);
        Package pkg = packageRepository.findById(packageId)
            .orElseThrow(() -> new RuntimeException("Package not found"));
        
        model.addAttribute("package", pkg);
        model.addAttribute("transactions", transactions);
        return "warehouse/package-transactions";
    }
}