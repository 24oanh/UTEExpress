// ltweb/entity/Order.java - Thêm các trường mới
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
    private OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    @JsonIgnoreProperties({"user"})
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "destination_warehouse_id")
    @JsonIgnoreProperties({"user"})
    private Warehouse destinationWarehouse;

    @ManyToOne
    @JoinColumn(name = "shipper_id")
    @JsonIgnoreProperties({"user"})
    private Shipper shipper;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "service_type")
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;
    
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column(name = "total_weight")
    private Double totalWeight;

    @Column(name = "total_distance")
    private Double totalDistance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = OrderStatus.CHO_GIAO;
        paymentStatus = PaymentStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}