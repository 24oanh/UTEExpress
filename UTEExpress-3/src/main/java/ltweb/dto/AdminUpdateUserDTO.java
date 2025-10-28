package ltweb.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateUserDTO {
    
    private Long id;
    
    @NotBlank(message = "Username không được để trống")
    private String username;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    private String phone;
    
    @NotNull(message = "Trạng thái không được để trống")
    private Boolean isActive;
    
    private List<Long> roleIds;
}