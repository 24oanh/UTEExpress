package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbound_receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String receiptCode;
    
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User receivedBy;
    
    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        receivedDate = LocalDateTime.now();
        status = ReceiptStatus.PENDING;
    }
}