package ltweb.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddressDTO {
    
    private Long id;
    
    @NotBlank(message = "Nhãn địa chỉ không được để trống")
    @Size(max = 50, message = "Nhãn địa chỉ không quá 50 ký tự")
    private String label;
    
    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Số điện thoại không hợp lệ")
    private String recipientPhone;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 10, max = 500, message = "Địa chỉ phải từ 10-500 ký tự")
    private String address;
    
    private Boolean isDefault;
}