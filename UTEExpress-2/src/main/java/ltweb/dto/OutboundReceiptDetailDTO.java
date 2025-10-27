package ltweb.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundReceiptDetailDTO {
    private String packageCode;
    private Integer quantity;
    private String notes;
}