package ltweb.dto;

import ltweb.entity.ReceiptStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundReceiptDTO {
    private Long id;
    private String receiptCode;
    private String orderCode;
    private String receivedBy;
    private LocalDateTime receivedDate;
    private ReceiptStatus status;
    private String notes;
    private Integer totalQuantity;
    private List<InboundReceiptDetailDTO> details;
}