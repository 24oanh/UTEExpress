package ltweb.controller;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerProfileController {
    
    private final CustomerService customerService;
    private final CustomerAddressService addressService;
    private final NotificationService notificationService;
    
    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("currentCustomer");
        model.addAttribute("customer", customer);
        model.addAttribute("updateProfileDTO", UpdateProfileDTO.builder()
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .build());
        return "customer/profile";
    }
    
    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute UpdateProfileDTO dto,
                              BindingResult result,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            model.addAttribute("customer", customer);
            model.addAttribute("updateProfileDTO", dto);
            return "customer/profile";
        }
        
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            Customer updated = customerService.updateProfile(customer.getId(), dto);
            session.setAttribute("currentCustomer", updated);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/customer/profile";
    }
    
    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        return "customer/change-password";
    }
    
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordDTO dto,
                                BindingResult result,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("changePasswordDTO", dto);
            return "customer/change-password";
        }
        
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            customerService.changePassword(customer.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");
            return "redirect:/customer/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("changePasswordDTO", dto);
            return "customer/change-password";
        }
    }
    
    @GetMapping("/addresses")
    public String addresses(Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("currentCustomer");
        List<CustomerAddress> addresses = addressService.getAddressesByCustomerId(customer.getId());
        model.addAttribute("addresses", addresses);
        return "customer/addresses";
    }
    
    @GetMapping("/addresses/add")
    public String addAddressForm(Model model) {
        model.addAttribute("addressDTO", new CustomerAddressDTO());
        model.addAttribute("isEdit", false);
        return "customer/address-form";
    }
    
    @PostMapping("/addresses/add")
    public String addAddress(@Valid @ModelAttribute("addressDTO") CustomerAddressDTO dto,
                            BindingResult result,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("addressDTO", dto);
            model.addAttribute("isEdit", false);
            return "customer/address-form";
        }
        
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            addressService.createAddress(customer.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Thêm địa chỉ thành công");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("addressDTO", dto);
            model.addAttribute("isEdit", false);
            return "customer/address-form";
        }
        
        return "redirect:/customer/addresses";
    }
    
    @GetMapping("/addresses/{id}/edit")
    public String editAddressForm(@PathVariable Long id, Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("currentCustomer");
        CustomerAddress address = addressService.getAddressById(id);
        
        if (!address.getCustomer().getId().equals(customer.getId())) {
            return "redirect:/customer/addresses";
        }
        
        CustomerAddressDTO dto = CustomerAddressDTO.builder()
                .id(address.getId())
                .label(address.getLabel())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .address(address.getAddress())
                .isDefault(address.getIsDefault())
                .build();
        
        model.addAttribute("addressDTO", dto);
        model.addAttribute("isEdit", true);
        return "customer/address-form";
    }
    
    @PostMapping("/addresses/{id}/update")
    public String updateAddress(@PathVariable Long id,
                              @Valid @ModelAttribute("addressDTO") CustomerAddressDTO dto,
                              BindingResult result,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "customer/address-form";
        }
        
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            addressService.updateAddress(customer.getId(), id, dto);
            redirectAttributes.addFlashAttribute("success", "Cập nhật địa chỉ thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/customer/addresses";
    }
    
    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            addressService.deleteAddress(customer.getId(), id);
            redirectAttributes.addFlashAttribute("success", "Xóa địa chỉ thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/customer/addresses";
    }
    
    @PostMapping("/addresses/{id}/set-default")
    public String setDefaultAddress(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            addressService.setDefaultAddress(customer.getId(), id);
            redirectAttributes.addFlashAttribute("success", "Đặt địa chỉ mặc định thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/customer/addresses";
    }
    
    @PostMapping("/profile/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile avatar,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (avatar.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ảnh");
            return "redirect:/customer/profile";
        }
        
        // Kiểm tra kích thước file (max 5MB)
        if (avatar.getSize() > 5 * 1024 * 1024) {
            redirectAttributes.addFlashAttribute("error", "Kích thước ảnh không được vượt quá 5MB");
            return "redirect:/customer/profile";
        }
        
        // Kiểm tra định dạng file
        String contentType = avatar.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            redirectAttributes.addFlashAttribute("error", "Chỉ chấp nhận file ảnh");
            return "redirect:/customer/profile";
        }
        
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            Customer updated = customerService.updateAvatar(customer.getId(), avatar);
            session.setAttribute("currentCustomer", updated);
            redirectAttributes.addFlashAttribute("success", "Cập nhật avatar thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/customer/profile";
    }

    @PostMapping("/profile/delete-avatar")
    public String deleteAvatar(HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            customerService.deleteAvatar(customer.getId());
            customer.setAvatarUrl(null);
            session.setAttribute("currentCustomer", customer);
            redirectAttributes.addFlashAttribute("success", "Xóa avatar thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/customer/profile";
    }
    
    @GetMapping("/notifications")
    public String notifications(Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("currentCustomer");
        User user = customer.getUser();
        
        List<Notification> notifications = notificationService.getNotificationsByUserId(user.getId());
        long unreadCount = notificationService.countUnreadNotifications(user.getId());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "customer/notifications";
    }


    @PostMapping("/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpSession session) {
        Customer customer = (Customer) session.getAttribute("currentCustomer");
        User user = customer.getUser();
        long count = notificationService.countUnreadNotifications(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}