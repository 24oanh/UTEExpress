package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_type", nullable = false)
    private String recipientType; // ADMIN, WAREHOUSE, SHIPPER, CUSTOMER

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isRead = false;
    }
}