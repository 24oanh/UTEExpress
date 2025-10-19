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

    @Column(nullable = false)
    private String recipientType; // WAREHOUSE, SHIPPER

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type; // ORDER_CREATED, ORDER_ASSIGNED, ORDER_COMPLETED, ORDER_FAILED

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isRead = false;
    }
}
