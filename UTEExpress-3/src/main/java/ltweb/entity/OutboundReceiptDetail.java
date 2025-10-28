package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "outbound_receipt_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundReceiptDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "outbound_receipt_id", nullable = false)
    private OutboundReceipt outboundReceipt;
    
    @ManyToOne
    @JoinColumn(name = "package_id", nullable = false)
    private Package packageItem;
    
    @ManyToOne
    @JoinColumn(name = "warehouse_location_id")
    private WarehouseLocation warehouseLocation;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;
}