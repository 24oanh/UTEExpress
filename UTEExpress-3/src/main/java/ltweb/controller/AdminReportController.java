package ltweb.controller;

import ltweb.entity.*;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ltweb.repository.ShipperRepository;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReportController {
    
    private final OrderService orderService;
    private final ShipperRepository shipperRepository;
    private final UserService userService;

    @GetMapping
    public String reportsPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate,
            @RequestParam(required = false) String period,
            Model model) {
        
        // Get all orders
        List<Order> allOrders = orderService.getAllOrders();
        
        // Filter by date range if provided
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
            
            allOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(start) && o.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());
        }
        
        // Calculate statistics
        Map<String, Object> stats = calculateStatistics(allOrders);
        
        model.addAttribute("totalRevenue", stats.get("totalRevenue"));
        model.addAttribute("totalOrders", stats.get("totalOrders"));
        model.addAttribute("totalShippers", stats.get("totalShippers"));
        model.addAttribute("successRate", stats.get("successRate"));
        
        // Add filter parameters back to model
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("period", period);
        
        return "admin/reports";
    }
    
    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReportData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String endDate,
            @RequestParam(required = false) String period) {
        
        List<Order> allOrders = orderService.getAllOrders();
        
        // Filter by date range
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
            
            allOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(start) && o.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());
        }
        
        Map<String, Object> data = calculateStatistics(allOrders);
        
        // Add chart data
        data.put("revenueChart", generateRevenueChartData(allOrders, period));
        data.put("orderStatusChart", generateOrderStatusChartData(allOrders));
        data.put("serviceTypeChart", generateServiceTypeChartData(allOrders));
        data.put("shipperPerformance", generateShipperPerformanceData());
        
        return ResponseEntity.ok(data);
    }
    
    private Map<String, Object> calculateStatistics(List<Order> orders) {
        Map<String, Object> stats = new HashMap<>();
        
        // Total revenue (only completed orders)
        BigDecimal totalRevenue = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.HOAN_THANH)
            .map(Order::getShipmentFee)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", orders.size());
        
        // Active shippers count
        long activeShippers = userService.countUsersByRole("ROLE_SHIPPER");
        stats.put("totalShippers", activeShippers);
        
        // Success rate
        long completedOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.HOAN_THANH)
            .count();
        
        double successRate = orders.isEmpty() ? 0.0 : 
            (completedOrders * 100.0 / orders.size());
        
        stats.put("successRate", String.format("%.1f", successRate));
        
        return stats;
    }
    
    private Map<String, Object> generateRevenueChartData(List<Order> orders, String period) {
        Map<String, Object> chartData = new HashMap<>();
        
        // Group orders by month
        Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        
        orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.HOAN_THANH)
            .forEach(order -> {
                String month = order.getCreatedAt().getMonth().toString().substring(0, 3);
                monthlyRevenue.merge(month, order.getShipmentFee(), BigDecimal::add);
            });
        
        chartData.put("labels", new ArrayList<>(monthlyRevenue.keySet()));
        chartData.put("data", new ArrayList<>(monthlyRevenue.values()));
        
        return chartData;
    }
    
    private Map<String, Object> generateOrderStatusChartData(List<Order> orders) {
        Map<String, Object> chartData = new HashMap<>();
        
        Map<String, Long> statusCount = orders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getStatus().name(),
                Collectors.counting()
            ));
        
        List<String> labels = Arrays.asList("Hoàn thành", "Đang giao", "Chờ giao", "Thất bại", "Đã hủy");
        List<Long> data = Arrays.asList(
            statusCount.getOrDefault("HOAN_THANH", 0L),
            statusCount.getOrDefault("DANG_GIAO", 0L),
            statusCount.getOrDefault("CHO_GIAO", 0L),
            statusCount.getOrDefault("THAT_BAI", 0L),
            statusCount.getOrDefault("HUY", 0L)
        );
        
        chartData.put("labels", labels);
        chartData.put("data", data);
        
        return chartData;
    }
    
    private Map<String, Object> generateServiceTypeChartData(List<Order> orders) {
        Map<String, Object> chartData = new HashMap<>();
        
        Map<String, Long> serviceCount = orders.stream()
            .collect(Collectors.groupingBy(
                o -> o.getServiceType().name(),
                Collectors.counting()
            ));
        
        List<String> labels = Arrays.asList("Nhanh", "Chuẩn", "Tiết kiệm");
        List<Long> data = Arrays.asList(
            serviceCount.getOrDefault("EXPRESS", 0L),
            serviceCount.getOrDefault("STANDARD", 0L),
            serviceCount.getOrDefault("ECONOMY", 0L)
        );
        
        chartData.put("labels", labels);
        chartData.put("data", data);
        
        return chartData;
    }
    
    private List<Map<String, Object>> generateShipperPerformanceData() {
        List<Map<String, Object>> performance = new ArrayList<>();
        
        List<Shipper> shippers = shipperRepository.findByIsActive(true);
        
        for (Shipper shipper : shippers) {
            Map<String, Object> shipperData = new HashMap<>();
            shipperData.put("name", shipper.getName());
            shipperData.put("total", shipper.getTotalDeliveries());
            shipperData.put("success", shipper.getSuccessfulDeliveries());
            shipperData.put("failed", shipper.getFailedDeliveries());
            
            double rate = shipper.getTotalDeliveries() == 0 ? 0.0 :
                (shipper.getSuccessfulDeliveries() * 100.0 / shipper.getTotalDeliveries());
            
            shipperData.put("rate", String.format("%.1f", rate));
            
            // Determine rating
            String rating;
            if (rate >= 95) rating = "excellent";
            else if (rate >= 90) rating = "good";
            else if (rate >= 80) rating = "average";
            else rating = "poor";
            
            shipperData.put("rating", rating);
            
            performance.add(shipperData);
        }
        
        // Sort by success rate
        performance.sort((a, b) -> 
            Double.compare(
                Double.parseDouble((String) b.get("rate")),
                Double.parseDouble((String) a.get("rate"))
            )
        );
        
        return performance.stream().limit(10).collect(Collectors.toList());
    }
}