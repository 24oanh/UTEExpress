// PaymentController.java - SỬA LẠI HOÀN TOÀN
package ltweb.controller;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.repository.OrderRepository;
import ltweb.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @GetMapping("/customer/payment/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String paymentPage(@PathVariable Long orderId, Model model, HttpSession session) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

            Customer customer = (Customer) session.getAttribute("currentCustomer");
            if (customer == null || !order.getCustomer().getId().equals(customer.getId())) {
                return "redirect:/customer/orders";
            }

            model.addAttribute("orderId", orderId);
            model.addAttribute("order", order);

            return "customer/payment";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/orders";
        }
    }

    @PostMapping("/customer/payment/process")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String processPayment(@RequestParam Long orderId,
            @RequestParam String paymentMethod,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            if (customer == null) {
                redirectAttributes.addFlashAttribute("error", "Chưa đăng nhập");
                return "redirect:/login";
            }

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

            if (!order.getCustomer().getId().equals(customer.getId())) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền");
                return "redirect:/customer/orders";
            }

            PaymentRequestDTO dto = PaymentRequestDTO.builder()
                    .orderId(orderId)
                    .paymentMethod(PaymentMethod.valueOf(paymentMethod))
                    .build();

            PaymentResponseDTO response = paymentService.createPayment(dto);

            if (response.getSuccess()) {
                redirectAttributes.addFlashAttribute("success", response.getMessage());
                return "redirect:/customer/orders/" + orderId;
            } else {
                redirectAttributes.addFlashAttribute("error", response.getMessage());
                return "redirect:/customer/payment/" + orderId;
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý thanh toán: " + e.getMessage());
            return "redirect:/customer/payment/" + orderId;
        }
    }

    @GetMapping("/customer/payment/callback")
    public String paymentCallback(@RequestParam(required = false) String paymentCode,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {
        try {
            if ("success".equalsIgnoreCase(status)) {
                Payment payment = paymentService.confirmPayment(paymentCode, transactionId);
                redirectAttributes.addFlashAttribute("success",
                        "Thanh toán thành công! Hóa đơn đã được gửi qua email.");
                return "redirect:/customer/orders/" + payment.getOrder().getId();
            } else {
                if (paymentCode != null) {
                    paymentService.failPayment(paymentCode, "Thanh toán thất bại");
                }
                redirectAttributes.addFlashAttribute("error", "Thanh toán thất bại!");
                return "redirect:/customer/orders";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý thanh toán: " + e.getMessage());
            return "redirect:/customer/orders";
        }
    }

    @PostMapping("/api/payment/momo/ipn")
    @ResponseBody
    public ResponseEntity<?> momoIPN(@RequestBody String requestBody) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/payment/zalopay/callback")
    @ResponseBody
    public ResponseEntity<?> zaloPayCallback(@RequestBody String requestBody) {
        return ResponseEntity.ok().build();
    }
}