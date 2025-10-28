package ltweb.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCreateUserDTO {
    
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    private String username;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6-50 ký tự")
    private String password;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
    
    @NotEmpty(message = "Phải chọn ít nhất một vai trò")
    private List<Long> roleIds;
    
    private String address;
    private String vehicleType;
    private String vehicleNumber;
    private String warehouseName;
    private Integer totalCapacity;
}