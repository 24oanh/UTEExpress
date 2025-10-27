package ltweb.dto;

import ltweb.entity.PaymentMethod;
import lombok.*;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDTO {
    @NotNull
    private Long orderId;
    
    @NotNull
    private PaymentMethod paymentMethod;
    
    private String returnUrl;
    private String cancelUrl;
}