// ltweb/controller/CustomerAuthController.java

package ltweb.controller;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerAuthController {

	private final CustomerService customerService;
	private final AuthService authService;

	@GetMapping("/register")
	public String registerPage(Model model) {
	    model.addAttribute("registration", new CustomerRegistrationDTO());
	    model.addAttribute("forgotPasswordDTO", new ForgotPasswordDTO());
	    return "register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registration") CustomerRegistrationDTO dto, BindingResult result,
			Model model, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			model.addAttribute("registration", dto);
			return "customer/register";
		}

		try {
			customerService.registerCustomer(dto);
			redirectAttributes.addFlashAttribute("success",
					"Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
			return "redirect:/login";
		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
			model.addAttribute("registration", dto);
			return "customer/register";
		}
	}
	

	@GetMapping("/verify")
	public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
		try {
			customerService.verifyEmail(token);
			redirectAttributes.addFlashAttribute("success", "Xác thực email thành công! Bạn có thể đăng nhập ngay.");
			return "redirect:/login";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/login";
		}
	}

	@GetMapping("/resend-verification")
	public String resendVerificationPage(Model model) {
		return "customer/resend-verification";
	}

	@PostMapping("/resend-verification")
	public String resendVerification(@RequestParam String email, RedirectAttributes redirectAttributes) {
		try {
			customerService.resendVerificationEmail(email);
			redirectAttributes.addFlashAttribute("success",
					"Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư.");
			return "redirect:/login";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/customer/resend-verification";
		}
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model, Authentication auth, HttpSession session) {
		User user = authService.findByUsername(auth.getName());
		Customer customer = customerService.getCustomerByUserId(user.getId());

		session.setAttribute("currentUser", user);
		session.setAttribute("currentCustomer", customer);

		model.addAttribute("customer", customer);
		return "customer/dashboard";
	}

	@GetMapping("/forgot-password")
	public String forgotPasswordPage(Model model) {
		model.addAttribute("forgotPasswordDTO", new ForgotPasswordDTO());
		return "customer/forgot-password";
	}

	@PostMapping("/forgot-password")
	public String forgotPassword(@Valid @ModelAttribute ForgotPasswordDTO dto, BindingResult result, Model model,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			model.addAttribute("forgotPasswordDTO", dto);
			return "customer/forgot-password";
		}

		try {
			customerService.initiatePasswordReset(dto.getEmail());
			redirectAttributes.addFlashAttribute("success",
					"Email đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.");
			return "redirect:/login";
		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
			model.addAttribute("forgotPasswordDTO", dto);
			return "customer/forgot-password";
		}
	}

	@GetMapping("/reset-password")
	public String resetPasswordPage(@RequestParam String token, Model model) {
		ResetPasswordDTO dto = new ResetPasswordDTO();
		dto.setToken(token);
		model.addAttribute("resetPasswordDTO", dto);
		return "customer/reset-password";
	}

	@PostMapping("/reset-password")
	public String resetPassword(@Valid @ModelAttribute ResetPasswordDTO dto, BindingResult result, Model model,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			model.addAttribute("resetPasswordDTO", dto);
			return "customer/reset-password";
		}

		try {
			customerService.resetPassword(dto);
			redirectAttributes.addFlashAttribute("success",
					"Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới.");
			return "redirect:/login";
		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
			model.addAttribute("resetPasswordDTO", dto);
			return "customer/reset-password";
		}
	}

}