package ltweb.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundOutboundReportDTO {
    private Long warehouseId;
    private String warehouseName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    private Integer totalInboundReceipts;
    private Integer totalOutboundReceipts;
    
    private Map<LocalDate, Integer> inboundByDate;
    private Map<LocalDate, Integer> outboundByDate;
    
    private List<InboundReceiptDTO> inboundReceipts;
    private List<OutboundReceiptDTO> outboundReceipts;
}