package ltweb.dto;

import ltweb.entity.ServiceType;
import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderDTO {

    @NotBlank(message = "Tên người gửi không được để trống")
    private String senderName;

    @NotBlank(message = "Số điện thoại người gửi không được để trống")
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Số điện thoại không hợp lệ")
    private String senderPhone;

    @NotBlank(message = "Địa chỉ người gửi không được để trống")
    private String senderAddress;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Số điện thoại không hợp lệ")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ người nhận không được để trống")
    private String recipientAddress;

    @NotNull(message = "Loại dịch vụ không được để trống")
    private ServiceType serviceType;

    private String notes;

    @NotBlank(message = "Mô tả hàng hóa không được để trống")
    private String itemDescription;

    @NotNull(message = "Trọng lượng không được để trống")
    @DecimalMin(value = "0.1", message = "Trọng lượng phải lớn hơn 0")
    private Double weight;

    @NotNull(message = "Chiều dài không được để trống")
    @DecimalMin(value = "1", message = "Chiều dài phải lớn hơn 0")
    private Double length;

    @NotNull(message = "Chiều rộng không được để trống")
    @DecimalMin(value = "1", message = "Chiều rộng phải lớn hơn 0")
    private Double width;

    @NotNull(message = "Chiều cao không được để trống")
    @DecimalMin(value = "1", message = "Chiều cao phải lớn hơn 0")
    private Double height;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Kho đích không được để trống")
    private Long destinationWarehouseId;
}