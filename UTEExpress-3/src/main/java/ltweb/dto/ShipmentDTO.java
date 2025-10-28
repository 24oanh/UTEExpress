package ltweb.dto;

import ltweb.entity.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDTO {
    
    private Long id;
    
    private String shipmentCode;
    
    private Long orderId;
    
    private String orderCode;
    
    private Long shipperId;
    
    private String shipperName;
    
    private LocalDateTime pickupTime;
    
    private LocalDateTime deliveryTime;
    
    private String notes;
    
    private String proofImageUrl;
    
    private ShipmentStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}