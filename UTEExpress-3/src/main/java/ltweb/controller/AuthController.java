package ltweb.controller;

import ltweb.dto.RegisterDTO;
import ltweb.dto.ForgotPasswordDTO;
import ltweb.dto.ResetPasswordDTO;
import ltweb.entity.User;
import ltweb.service.AuthService;
import ltweb.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CustomerService customerService;

    @GetMapping("/")
    public String home(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser") ||
                auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        User user = authService.findByUsername(auth.getName());
        session.setAttribute("currentUser", user);

        for (org.springframework.security.core.GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority();
            switch (role) {
                case "ROLE_ADMIN":
                    return "redirect:/admin/dashboard";
                case "ROLE_WAREHOUSE_STAFF":
                    return "redirect:/warehouse/dashboard";
                case "ROLE_SHIPPER":
                    return "redirect:/shipper/dashboard";
                case "ROLE_CUSTOMER":
                    return "redirect:/customer/dashboard";
                case "ROLE_SUPPORT":
                    return "redirect:/support/chat";
            }
        }
        return "redirect:/login?error=true";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "register";
        }

        if (result.hasErrors()) {
            return "register";
        }

        try {
            authService.register(dto);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordDTO", new ForgotPasswordDTO());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute ForgotPasswordDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("forgotPasswordDTO", dto);
            return "forgot-password";
        }

        try {
            customerService.initiatePasswordReset(dto.getEmail());
            redirectAttributes.addFlashAttribute("success",
                    "Email đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("forgotPasswordDTO", dto);
            return "forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken(token);
        model.addAttribute("resetPasswordDTO", dto);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute ResetPasswordDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("resetPasswordDTO", dto);
            return "reset-password";
        }

        try {
            customerService.resetPassword(dto);
            redirectAttributes.addFlashAttribute("success",
                    "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("resetPasswordDTO", dto);
            return "reset-password";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}