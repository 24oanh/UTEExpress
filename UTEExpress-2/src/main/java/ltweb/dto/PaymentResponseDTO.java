package ltweb.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {
    private String paymentCode;
    private String paymentUrl;
    private String qrCodeUrl;
    private String message;
    private Boolean success;
}