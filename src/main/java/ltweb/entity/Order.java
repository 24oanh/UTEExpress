package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderCode;

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
    private BigDecimal shipmentFee;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status; // CHO_GIAO, DANG_GIAO, HOAN_THANH, THAT_BAI, HUY

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    @JsonIgnoreProperties({"user"})  
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "shipper_id")
    @JsonIgnoreProperties({"user"})  
    private Shipper shipper;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = OrderStatus.CHO_GIAO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}