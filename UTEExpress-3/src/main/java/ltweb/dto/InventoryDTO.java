package ltweb.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {
    private Long id;
    private String packageCode;
    private String packageDescription;
    private Integer quantity;
    private Integer deliveredQuantity;
    private Integer remainingQuantity;
    private String orderCode;
}