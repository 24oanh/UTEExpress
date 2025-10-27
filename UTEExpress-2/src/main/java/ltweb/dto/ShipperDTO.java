package ltweb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperDTO {
    
    private Long id;
    
    private String code;
    
    private String name;
    
    private String phone;
    
    private String email;
    
    private String vehicleType;
    
    private String vehicleNumber;
    
    private Boolean isActive;
    
    private Double currentLatitude;
    
    private Double currentLongitude;
    
    private Integer totalDeliveries;
    
    private Integer successfulDeliveries;
    
    private Integer failedDeliveries;
    
    private Long userId;
    
    private String username;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}