package ltweb.dto;

import ltweb.entity.ServiceType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryDTO {
    private String senderName;
    private String senderPhone;
    private String senderAddress;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private ServiceType serviceType;
    private String notes;
    private String itemDescription;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;
    private Integer quantity;
    private Double distance;
    private BigDecimal estimatedFee;
    private LocalDateTime estimatedDeliveryDate;
    private String destinationWarehouseName;
}