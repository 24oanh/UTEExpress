package ltweb.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundReceiptDetailDTO {
    private String packageCode;
    private Integer quantity;
    private String notes;
}