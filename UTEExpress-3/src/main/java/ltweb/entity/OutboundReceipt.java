package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundReceipt {
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
    @JoinColumn(name = "shipper_id")
    private Shipper shipper;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User issuedBy;
    
    @Column(name = "issued_date", nullable = false)
    private LocalDateTime issuedDate;
    
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
        issuedDate = LocalDateTime.now();
        status = ReceiptStatus.PENDING;
    }
}