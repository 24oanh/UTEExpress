package ltweb.service;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MomoPaymentService momoPaymentService;
    private final ZaloPayPaymentService zaloPayPaymentService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO dto) {
        Order order = orderRepository.findById(dto.getOrderId())
            .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        Payment payment = Payment.builder()
            .order(order)
            .amount(order.getShipmentFee())
            .paymentMethod(dto.getPaymentMethod())
            .build();

        payment = paymentRepository.save(payment);

        order.setPaymentMethod(dto.getPaymentMethod());

        if (dto.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PENDING);
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);
            orderRepository.save(order);

            if (order.getCustomer() != null) {
                notificationService.createCustomerNotification(
                    order.getCustomer().getId(),
                    "Đơn hàng " + order.getOrderCode() + " đã được xác nhận. Bạn sẽ thanh toán khi nhận hàng.",
                    NotificationType.ORDER_CREATED,
                    order
                );
            }

            return PaymentResponseDTO.builder()
                .paymentCode(payment.getPaymentCode())
                .message("Đơn hàng đã được xác nhận. Bạn sẽ thanh toán khi nhận hàng.")
                .success(true)
                .build();
        }

        // MOMO/ZALOPAY/BANK_CARD - Thanh toán ngay lập tức
        order.setPaymentStatus(PaymentStatus.PAID);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentDate(LocalDateTime.now());
        
        paymentRepository.save(payment);
        orderRepository.save(order);

        if (order.getCustomer() != null) {
            notificationService.createCustomerNotification(
                order.getCustomer().getId(),
                "Thanh toán đơn hàng " + order.getOrderCode() + " thành công. Đơn hàng đang được xử lý.",
                NotificationType.ORDER_CREATED,
                order
            );
        }

        PaymentResponseDTO response;
        if (dto.getPaymentMethod() == PaymentMethod.MOMO) {
            response = momoPaymentService.createPayment(payment, dto.getReturnUrl());
        } else if (dto.getPaymentMethod() == PaymentMethod.ZALOPAY) {
            response = zaloPayPaymentService.createPayment(payment, dto.getReturnUrl());
        } else {
            response = PaymentResponseDTO.builder()
                .paymentCode(payment.getPaymentCode())
                .message("Thanh toán thành công")
                .success(true)
                .build();
        }

        return response;
    }

    @Transactional
    public Payment confirmPayment(String paymentCode, String transactionId) {
        Payment payment = paymentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new RuntimeException("Thanh toán không tồn tại"));

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(transactionId);
        payment.setPaymentDate(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        sendInvoiceEmail(payment);
        
        // Thông báo cho customer
        if (order.getCustomer() != null) {
            notificationService.createCustomerNotification(
                order.getCustomer().getId(),
                "Thanh toán đơn hàng " + order.getOrderCode() + " thành công. Đơn hàng đang được xử lý.",
                NotificationType.ORDER_CREATED,
                order
            );
        }

        return payment;
    }

    @Transactional
    public Payment failPayment(String paymentCode, String reason) {
        Payment payment = paymentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new RuntimeException("Thanh toán không tồn tại"));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setNotes(reason);
        return paymentRepository.save(payment);
    }

    private void sendInvoiceEmail(Payment payment) {
        Order order = payment.getOrder();
        Customer customer = order.getCustomer();

        String subject = "Hóa đơn điện tử - " + order.getOrderCode();
        String message = buildInvoiceEmail(payment);

        emailService.sendEmail(customer.getEmail(), subject, message);
    }

    private String buildInvoiceEmail(Payment payment) {
        Order order = payment.getOrder();

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head><body>" +
                "<h2>HÓA ĐƠN ĐIỆN TỬ</h2>" +
                "<p><strong>Mã đơn hàng:</strong> " + order.getOrderCode() + "</p>" +
                "<p><strong>Mã thanh toán:</strong> " + payment.getPaymentCode() + "</p>" +
                "<p><strong>Ngày thanh toán:</strong> " + payment.getPaymentDate() + "</p>" +
                "<p><strong>Phương thức:</strong> " + getPaymentMethodName(payment.getPaymentMethod()) + "</p>" +
                "<hr>" +
                "<h3>Thông tin gửi hàng</h3>" +
                "<p><strong>Người gửi:</strong> " + order.getSenderName() + "</p>" +
                "<p><strong>Địa chỉ:</strong> " + order.getSenderAddress() + "</p>" +
                "<p><strong>Người nhận:</strong> " + order.getRecipientName() + "</p>" +
                "<p><strong>Địa chỉ:</strong> " + order.getRecipientAddress() + "</p>" +
                "<hr>" +
                "<h3>Chi phí</h3>" +
                "<p><strong>Phí vận chuyển:</strong> " + String.format("%,d", payment.getAmount().longValue()) + " đ</p>" +
                "<p><strong>Trạng thái:</strong> " + (payment.getPaymentMethod() == PaymentMethod.COD ? "Thanh toán khi nhận hàng" : "Đã thanh toán") + "</p>" +
                "<hr>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ UTEExpress!</p>" +
                "</body></html>";
    }

    private String getPaymentMethodName(PaymentMethod method) {
        switch (method) {
            case MOMO: return "Ví MoMo";
            case ZALOPAY: return "ZaloPay";
            case BANK_CARD: return "Thẻ ngân hàng";
            case COD: return "Thanh toán khi nhận hàng";
            default: return "Khác";
        }
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).orElse(null);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}