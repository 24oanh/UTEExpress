package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inbound_receipt_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundReceiptDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "inbound_receipt_id", nullable = false)
    private InboundReceipt inboundReceipt;
    
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