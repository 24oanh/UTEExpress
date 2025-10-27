package ltweb.controller;

import ltweb.dto.*;
import ltweb.entity.Customer;
import ltweb.entity.Order;
import ltweb.entity.Payment;
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

    // ✅ Route thanh toán
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
            
            System.out.println("=== PAYMENT PAGE ===");
            System.out.println("Order ID: " + orderId);
            System.out.println("Order Code: " + order.getOrderCode());
            
            return "customer/payment";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/orders";
        }
    }

    @PostMapping("/customer/payment/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseBody
    public ResponseEntity<PaymentResponseDTO> createPayment(@RequestBody PaymentRequestDTO dto) {
        try {
            System.out.println("=== CREATE PAYMENT ===");
            System.out.println("Order ID: " + dto.getOrderId());
            System.out.println("Payment Method: " + dto.getPaymentMethod());

            PaymentResponseDTO response = paymentService.createPayment(dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(PaymentResponseDTO.builder()
                .success(false)
                .message(e.getMessage())
                .build());
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