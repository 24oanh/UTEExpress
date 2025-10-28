package ltweb.dto;

import ltweb.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    
    private Long id;
    
    private String orderCode;
    
    private String senderName;
    
    private String senderPhone;
    
    private String senderAddress;
    
    private String recipientName;
    
    private String recipientPhone;
    
    private String recipientAddress;
    
    private BigDecimal shipmentFee;
    
    private String notes;
    
    private OrderStatus status;
    
    private Long warehouseId;
    
    private String warehouseName;
    
    private Long shipperId;
    
    private String shipperName;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}