package ltweb.service;

import ltweb.dto.PaymentResponseDTO;
import ltweb.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ZaloPayPaymentService {
    
    @Value("${payment.zalopay.app-id:2553}")
    private String appId;
    
    @Value("${payment.zalopay.key1:PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL}")
    private String key1;
    
    @Value("${payment.zalopay.key2:kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz}")
    private String key2;
    
    @Value("${payment.zalopay.endpoint:https://sb-openapi.zalopay.vn/v2/create}")
    private String endpoint;
    
    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;
    
    public PaymentResponseDTO createPayment(Payment payment, String returnUrl) {
        try {
            String appTransId = new SimpleDateFormat("yyMMdd").format(new Date()) + "_" + payment.getPaymentCode();
            long amount = payment.getAmount().longValue();
            String description = "Thanh toán đơn hàng " + payment.getOrder().getOrderCode();
            String callbackUrl = frontendUrl + "/api/payment/zalopay/callback";
            
            String embedData = "{}";
            String item = "[]";
            
            String data = appId + "|" + appTransId + "|" + payment.getOrder().getCustomer().getEmail() + "|" +
                amount + "|" + System.currentTimeMillis() + "|" + embedData + "|" + item;
            
            String mac = hmacSHA256(data, key1);
            
            // Simulate response for testing
            return PaymentResponseDTO.builder()
                .paymentCode(payment.getPaymentCode())
                .paymentUrl("https://sb-openapi.zalopay.vn/v2/pay?order=" + appTransId)
                .message("Tạo thanh toán ZaloPay thành công")
                .success(true)
                .build();
            
        } catch (Exception e) {
            return PaymentResponseDTO.builder()
                .success(false)
                .message("Lỗi tạo thanh toán ZaloPay: " + e.getMessage())
                .build();
        }
    }
    
    private String hmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}