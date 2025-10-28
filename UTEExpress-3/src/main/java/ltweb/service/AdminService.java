package ltweb.service;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final ShipperRepository shipperRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SupportStaffRepository supportStaffRepository;

    @Transactional
    public User createUser(AdminCreateUserDTO dto) {
        // Validate username
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        // Validate email
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Validate phone cho Shipper
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (shipperRepository.existsByPhone(dto.getPhone())) {
                throw new RuntimeException("Số điện thoại đã được sử dụng bởi shipper khác");
            }
        }

        // Lấy roles
        Set<Role> roles = new HashSet<>();
        for (Long roleId : dto.getRoleIds()) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
            roles.add(role);
        }

        // Tạo User
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .isActive(true)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        // Tạo profile tương ứng theo role
        for (Role role : roles) {
            RoleType roleType = role.getName();
            if (roleType == RoleType.ROLE_CUSTOMER) {
                createCustomerProfile(user, dto);
                break;
            } else if (roleType == RoleType.ROLE_SHIPPER) {
                createShipperProfile(user, dto);
                break;
            } else if (roleType == RoleType.ROLE_WAREHOUSE_STAFF) {
                createWarehouseProfile(user, dto);
                break;
            } else if (roleType == RoleType.ROLE_SUPPORT) {
                createSupportProfile(user, dto);
                break;
            }
        }

        // ✅ QUAN TRỌNG: Gửi email BÊN NGOÀI transaction
        final User savedUser = user;
        final String plainPassword = dto.getPassword();
        sendWelcomeEmailAsync(savedUser, plainPassword);

        return user;
    }

    // ✅ THÊM: Method async để gửi email
    @Async
    protected void sendWelcomeEmailAsync(User user, String password) {
        try {
            emailService.sendEmail(
                user.getEmail(),
                "Tài khoản UTEExpress",
                buildWelcomeEmail(user, password)
            );
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng transaction
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }

    @Transactional
    public User updateUser(Long id, AdminUpdateUserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setIsActive(dto.getIsActive());

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : dto.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // ✅ KHUYẾN NGHỊ: Soft delete - chỉ vô hiệu hóa thay vì xóa hẳn
        // Điều này giúp:
        // - Giữ lại lịch sử dữ liệu
        // - Tránh lỗi foreign key constraint
        // - Có thể khôi phục nếu cần
        
        user.setIsActive(false);
        userRepository.save(user);
        
        // Vô hiệu hóa các profile liên quan
        warehouseRepository.findByUserId(id).ifPresent(warehouse -> {
            // Có thể thêm logic vô hiệu hóa warehouse nếu cần
            // warehouse.setActive(false);
            // warehouseRepository.save(warehouse);
        });

        shipperRepository.findByUserId(id).ifPresent(shipper -> {
            shipper.setIsActive(false);
            shipperRepository.save(shipper);
        });

        customerRepository.findByUserId(id).ifPresent(customer -> {
            customer.setStatus(CustomerStatus.INACTIVE);
            customerRepository.save(customer);
        });

        supportStaffRepository.findByUserId(id).ifPresent(supportStaff -> {
            supportStaff.setIsActive(false);
            supportStaffRepository.save(supportStaff);
        });
    }

    // ✅ THÊM: Method để hard delete (chỉ dùng khi thực sự cần thiết)
    @Transactional
    public void permanentDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        try {
            // Xóa theo thứ tự: Profile -> User
            warehouseRepository.findByUserId(id).ifPresent(warehouse -> {
                warehouseRepository.delete(warehouse);
                warehouseRepository.flush(); // Đảm bảo xóa ngay lập tức
            });

            shipperRepository.findByUserId(id).ifPresent(shipper -> {
                shipperRepository.delete(shipper);
                shipperRepository.flush();
            });

            customerRepository.findByUserId(id).ifPresent(customer -> {
                customerRepository.delete(customer);
                customerRepository.flush();
            });

            supportStaffRepository.findByUserId(id).ifPresent(supportStaff -> {
                supportStaffRepository.delete(supportStaff);
                supportStaffRepository.flush();
            });

            // Xóa User cuối cùng
            userRepository.delete(user);
            userRepository.flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Không thể xóa user do còn dữ liệu liên quan: " + e.getMessage());
        }
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    @Transactional
    public String resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // ✅ Gửi email async
        sendResetPasswordEmailAsync(user, newPassword);

        return newPassword;
    }

    // ✅ THÊM: Method async để gửi email reset password
    @Async
    protected void sendResetPasswordEmailAsync(User user, String newPassword) {
        try {
            emailService.sendEmail(
                user.getEmail(),
                "Đặt lại mật khẩu UTEExpress",
                buildResetPasswordEmail(user, newPassword)
            );
        } catch (Exception e) {
            System.err.println("Failed to send reset password email: " + e.getMessage());
        }
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        Set<Role> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
            roles.add(role);
        }

        user.setRoles(roles);
        userRepository.save(user);
    }

   

    private void createSupportProfile(User user, AdminCreateUserDTO dto) {
        SupportStaff supportStaff = SupportStaff.builder()
                .code("SUP" + System.currentTimeMillis())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .user(user)
                .isOnline(false)
                .isActive(true)
                .build();
        supportStaffRepository.save(supportStaff);
    }

    private void createCustomerProfile(User user, AdminCreateUserDTO dto) {
        Customer customer = Customer.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(dto.getAddress() != null ? dto.getAddress() : "")
                .user(user)
                .isEmailVerified(true)
                .status(CustomerStatus.ACTIVE)
                .build();
        customerRepository.save(customer);
    }

    private void createShipperProfile(User user, AdminCreateUserDTO dto) {
        // ✅ Sử dụng giá trị mặc định nếu không có
        String vehicleType = dto.getVehicleType();
        String vehicleNumber = dto.getVehicleNumber();
        
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            vehicleType = "Truck"; // Giá trị mặc định
        }
        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            vehicleNumber = "N/A"; // Sẽ cập nhật sau
        }

        Shipper shipper = Shipper.builder()
                .code("SHP" + System.currentTimeMillis())
                .name(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .vehicleType(vehicleType)
                .vehicleNumber(vehicleNumber)
                .user(user)
                .isActive(true)
                .build();
        shipperRepository.save(shipper);
    }

    private void createWarehouseProfile(User user, AdminCreateUserDTO dto) {
        // ✅ CHỈ validate khi có giá trị được nhập
        String warehouseName = dto.getWarehouseName();
        String address = dto.getAddress();
        
        // Nếu không có thông tin, sử dụng giá trị mặc định
        if (warehouseName == null || warehouseName.trim().isEmpty()) {
            warehouseName = "Warehouse " + user.getFullName();
        }
        if (address == null || address.trim().isEmpty()) {
            address = "Chưa cập nhật địa chỉ";
        }

        Warehouse warehouse = Warehouse.builder()
                .code("WH" + System.currentTimeMillis())
                .name(warehouseName)
                .address(address)
                .phone(user.getPhone())
                .email(user.getEmail())
                .manager(user.getFullName())
                .totalCapacity(dto.getTotalCapacity() != null ? dto.getTotalCapacity() : 10000)
                .currentStock(0)
                .user(user)
                .build();
        warehouseRepository.save(warehouse);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    private String buildWelcomeEmail(User user, String password) {
        return "<!DOCTYPE html><html><body>" +
                "<h2>Chào mừng đến với UTEExpress</h2>" +
                "<p>Tài khoản của bạn đã được tạo thành công.</p>" +
                "<p><strong>Tên đăng nhập:</strong> " + user.getUsername() + "</p>" +
                "<p><strong>Mật khẩu:</strong> " + password + "</p>" +
                "<p>Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu.</p>" +
                "</body></html>";
    }

    private String buildResetPasswordEmail(User user, String newPassword) {
        return "<!DOCTYPE html><html><body>" +
                "<h2>Đặt lại mật khẩu</h2>" +
                "<p>Mật khẩu của bạn đã được đặt lại.</p>" +
                "<p><strong>Mật khẩu mới:</strong> " + newPassword + "</p>" +
                "<p>Vui lòng đổi mật khẩu sau khi đăng nhập.</p>" +
                "</body></html>";
    }
}