package ltweb.controller;

import ltweb.dto.AdminCreateUserDTO;
import ltweb.dto.AdminUpdateUserDTO;
import ltweb.entity.RoleType;
import ltweb.entity.User;
import ltweb.service.AdminService;
import ltweb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        
        List<User> users;
        
        if (search != null && !search.isEmpty()) {
            users = userService.searchUsers(search);
        } else if (role != null && !role.isEmpty()) {
            users = userService.getUsersByRole(role);
        } else if (status != null && !status.isEmpty()) {
            users = userService.getUsersByStatus(Boolean.parseBoolean(status));
        } else {
            users = userService.getAllUsers();
        }
        
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "admin/user-detail";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("userDTO", new AdminCreateUserDTO());
        model.addAttribute("roles", RoleType.values());
        
        // Thêm mô tả cho từng role
        Map<String, String> roleDescriptions = new HashMap<>();
        roleDescriptions.put("ROLE_ADMIN", "Quản trị viên hệ thống");
        roleDescriptions.put("ROLE_CUSTOMER", "Khách hàng sử dụng dịch vụ");
        roleDescriptions.put("ROLE_SHIPPER", "Người giao hàng");
        roleDescriptions.put("ROLE_WAREHOUSE_STAFF", "Nhân viên kho");
        roleDescriptions.put("ROLE_SUPPORT", "Nhân viên hỗ trợ");
        model.addAttribute("roleDescriptions", roleDescriptions);
        
        return "admin/user-form";
    }

    @PostMapping("/create")
    public String createUser(
            @Valid @ModelAttribute("userDTO") AdminCreateUserDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("roles", RoleType.values());
            return "admin/user-form";
        }
        
        try {
            User user = adminService.createUser(dto);
            redirectAttributes.addFlashAttribute("success", 
                "Tạo user thành công! Email thông tin đăng nhập đã được gửi đến " + user.getEmail());
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", RoleType.values());
            return "admin/user-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        
        AdminUpdateUserDTO dto = AdminUpdateUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .roleIds(user.getRoles().stream()
                        .map(role -> (long) role.getName().ordinal())
                        .toList())
                .build();
        
        model.addAttribute("userDTO", dto);
        model.addAttribute("roles", RoleType.values());
        return "admin/user-edit";
    }

    @PostMapping("/{id}/update")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("userDTO") AdminUpdateUserDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("roles", RoleType.values());
            return "admin/user-edit";
        }
        
        try {
            adminService.updateUser(id, dto);
            redirectAttributes.addFlashAttribute("success", "Cập nhật user thành công!");
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", RoleType.values());
            return "admin/user-edit";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("success", "Thay đổi trạng thái user thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            String newPassword = adminService.resetPassword(id);
            redirectAttributes.addFlashAttribute("success", 
                "Reset mật khẩu thành công! Mật khẩu mới: " + newPassword);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    // ✅ THÊM: Soft delete (vô hiệu hóa user)
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(id); // Soft delete
            redirectAttributes.addFlashAttribute("success", "Đã vô hiệu hóa user thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ✅ THÊM: Hard delete (xóa vĩnh viễn)
    @PostMapping("/{id}/permanent-delete")
    public String permanentDeleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.permanentDeleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa vĩnh viễn user thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ✅ THÊM: Assign roles
    @PostMapping("/{id}/assign-roles")
    public String assignRoles(
            @PathVariable Long id,
            @RequestParam List<Long> roleIds,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.assignRoles(id, roleIds);
            redirectAttributes.addFlashAttribute("success", "Cập nhật vai trò thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
}