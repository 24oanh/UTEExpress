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
public class WarehouseDTO {
    
    private Long id;
    
    private String code;
    
    private String name;
    
    private String address;
    
    private String phone;
    
    private String email;
    
    private String manager;
    
    private Integer totalCapacity;
    
    private Integer currentStock;
    
    private Long userId;
    
    private String username;
    
    private LocalDateTime createdAt;
}