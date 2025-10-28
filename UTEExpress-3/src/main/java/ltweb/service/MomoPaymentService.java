package ltweb.service;

import ltweb.dto.PaymentResponseDTO;
import ltweb.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MomoPaymentService {
    
    @Value("${payment.momo.partner-code:MOMOBKUN20180529}")
    private String partnerCode;
    
    @Value("${payment.momo.access-key:klm05TvNBzhg7h7j}")
    private String accessKey;
    
    @Value("${payment.momo.secret-key:at67qH6mk8w5Y1nAyMoYKMWACiEi2bsa}")
    private String secretKey;
    
    @Value("${payment.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String endpoint;
    
    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;
    
    public PaymentResponseDTO createPayment(Payment payment, String returnUrl) {
        try {
            String orderId = payment.getPaymentCode();
            String requestId = orderId;
            long amount = payment.getAmount().longValue();
            String orderInfo = "Thanh toán đơn hàng " + payment.getOrder().getOrderCode();
            String redirectUrl = returnUrl != null ? returnUrl : frontendUrl + "/customer/payment/callback";
            String ipnUrl = frontendUrl + "/api/payment/momo/ipn";
            String requestType = "captureWallet";
            
            String rawHash = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;
            
            String signature = hmacSHA256(rawHash, secretKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", redirectUrl);
            requestBody.put("ipnUrl", ipnUrl);
            requestBody.put("requestType", requestType);
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");
            
            // Simulate response for testing
            return PaymentResponseDTO.builder()
                .paymentCode(payment.getPaymentCode())
                .paymentUrl("https://test-payment.momo.vn/v2/gateway/pay?t=" + orderId)
                .message("Tạo thanh toán MoMo thành công")
                .success(true)
                .build();
            
        } catch (Exception e) {
            return PaymentResponseDTO.builder()
                .success(false)
                .message("Lỗi tạo thanh toán MoMo: " + e.getMessage())
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