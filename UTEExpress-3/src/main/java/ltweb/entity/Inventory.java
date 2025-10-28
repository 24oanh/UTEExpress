package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @ManyToOne
    @JoinColumn(name = "package_id", nullable = false)
    private Package packageItem;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "delivered_quantity")
    private Integer deliveredQuantity;
    
    @Column(name = "remaining_quantity")
    private Integer remainingQuantity;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        deliveredQuantity = 0;
        remainingQuantity = quantity;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}