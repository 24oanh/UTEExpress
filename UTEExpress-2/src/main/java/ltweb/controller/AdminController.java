package ltweb.controller;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final UserService userService;
    private final OrderService orderService;
    private final TrackingService trackingService;
    private final ChatService chatService;
    private final NotificationService notificationService;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User admin = (User) session.getAttribute("currentUser");
        
        // User statistics
        long totalUsers = userService.countAllUsers();
        long totalCustomers = userService.countUsersByRole("ROLE_CUSTOMER");
        long totalShippers = userService.countUsersByRole("ROLE_SHIPPER");
        long totalWarehouseStaff = userService.countUsersByRole("ROLE_WAREHOUSE_STAFF");
        long activeUsers = userService.countActiveUsers();
        
        // ✅ THÊM: Order statistics
        List<Order> allOrders = orderService.getAllOrders();
        List<Order> recentOrders = allOrders.stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(10)
                .collect(Collectors.toList());
        long totalOrders = allOrders.size();
        long pendingOrders = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.CHO_GIAO)
            .count();
        long inProgressOrders = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DANG_GIAO)
            .count();
        long completedOrders = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.HOAN_THANH)
            .count();
        long failedOrders = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.THAT_BAI || o.getStatus() == OrderStatus.HUY)
            .count();
        
        // ✅ THÊM: Revenue statistics
        BigDecimal totalRevenue = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.HOAN_THANH)
            .map(Order::getShipmentFee)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Chat & Notifications
        long unreadChats = chatService.getTotalUnreadForAdmin();
        long unreadNotifications = notificationService.countUnreadNotifications(admin.getId());
        
        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalShippers", totalShippers);
        model.addAttribute("totalWarehouseStaff", totalWarehouseStaff);
        model.addAttribute("activeUsers", activeUsers);
        
        // ✅ THÊM: Order statistics to model
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("inProgressOrders", inProgressOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("failedOrders", failedOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        
        model.addAttribute("unreadChats", unreadChats);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("recentOrders", recentOrders);

        
        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String role,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String search,
                           Model model) {
        List<User> users;
        
        if (role != null && !role.isEmpty()) {
            users = userService.getUsersByRole(role);
        } else if (status != null && !status.isEmpty()) {
            users = userService.getUsersByStatus(Boolean.parseBoolean(status));
        } else if (search != null && !search.isEmpty()) {
            users = userService.searchUsers(search);
        } else {
            users = userService.getAllUsers();
        }
        
        model.addAttribute("users", users);
        
        return "admin/users";
    }
    
    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "admin/user-detail";
    }
    
    @GetMapping("/users/create")
    public String createUserForm(Model model) {
        model.addAttribute("userDTO", new AdminCreateUserDTO());
        model.addAttribute("roles", RoleType.values());
        return "admin/user-form";
    }
    
    @PostMapping("/users/create")
    public String createUser(@Valid @ModelAttribute AdminCreateUserDTO dto,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roles", RoleType.values());
            return "admin/user-form";
        }
        
        try {
            adminService.createUser(dto);
            redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành công");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", RoleType.values());
            return "admin/user-form";
        }
    }
    
    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        AdminUpdateUserDTO dto = AdminUpdateUserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .isActive(user.getIsActive())
            .roleIds(user.getRoles().stream().map(Role::getId).toList())
            .build();
        
        model.addAttribute("userDTO", dto);
        model.addAttribute("roles", RoleType.values());
        return "admin/user-edit";
    }
    
    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id,
                            @Valid @ModelAttribute AdminUpdateUserDTO dto,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roles", RoleType.values());
            return "admin/user-edit";
        }
        
        try {
            adminService.updateUser(id, dto);
            redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công");
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", RoleType.values());
            return "admin/user-edit";
        }
    }
    
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
    
    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            String newPassword = adminService.resetPassword(id);
            redirectAttributes.addFlashAttribute("success", "Mật khẩu mới: " + newPassword);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
    
    @GetMapping("/users/{id}/assign-roles")
    public String assignRolesForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("allRoles", RoleType.values());
        return "admin/assign-roles";
    }
    
    @PostMapping("/users/{id}/assign-roles")
    public String assignRoles(@PathVariable Long id,
                             @RequestParam List<Long> roleIds,
                             RedirectAttributes redirectAttributes) {
        try {
            adminService.assignRoles(id, roleIds);
            redirectAttributes.addFlashAttribute("success", "Gán quyền thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
    
    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search,
            Model model) {
        
        List<Order> orders = orderService.getAllOrders();
        
        model.addAttribute("orders", orders);
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        List<Package> packages = orderService.getPackagesByOrderId(id);
        Shipment shipment = orderService.getShipmentByOrderId(id);
        List<Tracking> trackings = new ArrayList<>();
        
        if (shipment != null) {
            trackings = trackingService.getTrackingsByShipmentId(shipment.getId());
        }
        
        model.addAttribute("order", order);
        model.addAttribute("packages", packages);
        model.addAttribute("trackings", trackings);
        
        return "admin/order-detail";
    }
    
    
    
    @GetMapping("/warehouses")
    public String warehouses(Model model) {
        // List<Warehouse> warehouses = warehouseService.findAll();
        // model.addAttribute("warehouses", warehouses);
        return "admin/warehouses";
    }
    @GetMapping("/warehouses/{id}")
    public String warehouseDetail(@PathVariable Long id, Model model) {
        model.addAttribute("warehouseId", id);
        return "admin/warehouse-detail";
    }
}