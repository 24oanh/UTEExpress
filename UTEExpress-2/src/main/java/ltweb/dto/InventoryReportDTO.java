package ltweb.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReportDTO {
    private Long warehouseId;
    private String warehouseName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer totalQuantity;
    private Integer totalDelivered;
    private Integer totalRemaining;
    private Integer totalInbound;
    private Integer totalOutbound;
    private Integer totalCapacity;
    private Double utilizationRate;

    private List<InventoryDTO> inventories;
}