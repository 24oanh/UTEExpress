package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;
    
    @Column(name = "total_inbound_receipts")
    private Integer totalInboundReceipts;
    
    @Column(name = "total_outbound_receipts")
    private Integer totalOutboundReceipts;
    
    @Column(name = "total_inbound_quantity")
    private Integer totalInboundQuantity;
    
    @Column(name = "total_outbound_quantity")
    private Integer totalOutboundQuantity;
    
    @Column(name = "opening_stock")
    private Integer openingStock;
    
    @Column(name = "closing_stock")
    private Integer closingStock;
    
    @Column(name = "utilization_rate")
    private Double utilizationRate;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User createdBy;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}