package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @Column(nullable = false, unique = true)
    private String locationCode;
    
    @Column(nullable = false)
    private String zone;
    
    @Column(nullable = false)
    private String rack;
    
    @Column(nullable = false)
    private String level;
    
    @Column(nullable = false)
    private String position;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private LocationStatus status;
    
    @ManyToOne
    @JoinColumn(name = "package_id")
    private Package packageItem;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = LocationStatus.EMPTY;
    }
}