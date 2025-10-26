package ltweb.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String senderPhone;

    @Column(nullable = false)
    private String senderAddress;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String recipientAddress;

    @Column(nullable = false)
    private BigDecimal estimatedFee;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String packageDescription;

    @Column(nullable = false)
    private Double totalWeight;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "from_warehouse_code")
    private String fromWarehouseCode;

    @Column(name = "to_warehouse_code")
    private String toWarehouseCode;

    @Column(name = "is_processed")
    private Boolean isProcessed;

    @Column(name = "processed_order_id")
    private Long processedOrderId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isProcessed = false;
    }
}