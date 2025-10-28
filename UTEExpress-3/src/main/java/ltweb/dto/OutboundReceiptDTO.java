package ltweb.dto;

import ltweb.entity.ReceiptStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundReceiptDTO {
    private Long id;
    private String receiptCode;
    private String orderCode;
    private String shipperName;
    private String issuedBy;
    private LocalDateTime issuedDate;
    private ReceiptStatus status;
    private String notes;
    private Integer totalQuantity;
    private List<OutboundReceiptDetailDTO> details;
}