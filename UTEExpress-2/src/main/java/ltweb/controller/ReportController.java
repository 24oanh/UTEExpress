package ltweb.controller;

import ltweb.entity.ReportType;
import ltweb.entity.User;
import ltweb.entity.Warehouse;
import ltweb.entity.WarehouseReport;
import ltweb.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/warehouse/reports")
@PreAuthorize("hasRole('WAREHOUSE_STAFF')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public String listReports(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<WarehouseReport> reports = reportService.getReportsByWarehouseId(warehouse.getId());
        model.addAttribute("reports", reports);
        return "warehouse/reports";
    }

    @GetMapping("/{id}")
    public String reportDetail(@PathVariable Long id, Model model) {
        WarehouseReport report = reportService.getReportById(id);
        model.addAttribute("report", report);
        return "warehouse/report-detail";
    }

    @GetMapping("/generate")
    public String generateReportForm(Model model) {
        model.addAttribute("reportTypes", ReportType.values());
        return "warehouse/report-form";
    }

    @PostMapping("/generate")
    public String generateReport(@RequestParam ReportType reportType,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                @RequestParam(required = false) String notes,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");
            
            reportService.generateReport(warehouse.getId(), reportType, startDate, endDate, user, notes);
            redirectAttributes.addFlashAttribute("success", "Report generated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate report: " + e.getMessage());
        }
        return "redirect:/warehouse/reports";
    }

    @PostMapping("/generate-daily")
    public String generateDailyReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");
            
            reportService.generateDailyReport(warehouse.getId(), date, user);
            redirectAttributes.addFlashAttribute("success", "Daily report generated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate daily report: " + e.getMessage());
        }
        return "redirect:/warehouse/reports";
    }

    @PostMapping("/generate-weekly")
    public String generateWeeklyReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime weekStart,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");
            
            reportService.generateWeeklyReport(warehouse.getId(), weekStart, user);
            redirectAttributes.addFlashAttribute("success", "Weekly report generated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate weekly report: " + e.getMessage());
        }
        return "redirect:/warehouse/reports";
    }

    @PostMapping("/generate-monthly")
    public String generateMonthlyReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime month,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");
            
            reportService.generateMonthlyReport(warehouse.getId(), month, user);
            redirectAttributes.addFlashAttribute("success", "Monthly report generated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate monthly report: " + e.getMessage());
        }
        return "redirect:/warehouse/reports";
    }

    @PostMapping("/generate-quarterly")
    public String generateQuarterlyReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime quarter,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");
            
            reportService.generateQuarterlyReport(warehouse.getId(), quarter, user);
            redirectAttributes.addFlashAttribute("success", "Quarterly report generated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate quarterly report: " + e.getMessage());
        }
        return "redirect:/warehouse/reports";
    }

    @PostMapping("/generate-yearly")
    public String generateYearlyReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime year,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
            User user = (User) session.getAttribute("currentUser");
            
            reportService.generateYearlyReport(warehouse.getId(), year, user);
            redirectAttributes.addFlashAttribute("success", "Yearly report generated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate yearly report: " + e.getMessage());
        }
        return "redirect:/warehouse/reports";
    }

    @PostMapping("/{id}/delete")
    public String deleteReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reportService.deleteReport(id);
            redirectAttributes.addFlashAttribute("success", "Report deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete report: " + e.getMessage());
        }
        return "redirect:/warehouse/reports";
    }
}