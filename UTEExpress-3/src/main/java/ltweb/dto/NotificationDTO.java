package ltweb.dto;

import ltweb.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    
    private Long id;
    
    private String recipientType;
    
    private Long recipientId;
    
    private String message;
    
    private Boolean isRead;
    
    private NotificationType type;
    
    private Long orderId;
    
    private String orderCode;
    
    private LocalDateTime createdAt;
}