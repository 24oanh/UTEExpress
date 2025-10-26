package ltweb.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerLoginDTO {
    
    @NotBlank(message = "Email/Số điện thoại không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    private Boolean rememberMe;
}