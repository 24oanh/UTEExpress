package ltweb.controller;

import ltweb.entity.User;
import ltweb.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "auth/login";
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            User user = authService.findByUsername(auth.getName());
            session.setAttribute("currentUser", user);
            
            if (user.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_WAREHOUSE_STAFF"))) {
                return "redirect:/warehouse/dashboard";
            } else if (user.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_SHIPPER"))) {
                return "redirect:/shipper/dashboard";
            }
        }
        return "redirect:/login";
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