package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @ManyToOne
    @JoinColumn(name = "package_id")
    private Package packageItem;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    
    @Column(name = "old_status", columnDefinition = "NVARCHAR(MAX)")
    private String oldStatus;
    
    @Column(name = "new_status", columnDefinition = "NVARCHAR(MAX)")
    private String newStatus;
    
    @Column(name = "quantity_changed")
    private Integer quantityChanged;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User performedBy;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}