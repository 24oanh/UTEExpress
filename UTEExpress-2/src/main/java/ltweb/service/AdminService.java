package ltweb.service;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    
    @Transactional
    public User createUser(AdminCreateUserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        
        Set<Role> roles = new HashSet<>();
        for (Long roleId : dto.getRoleIds()) {
            Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
            roles.add(role);
        }
        
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
        
        for (Role role : roles) {
            if (role.getName().equals("ROLE_CUSTOMER")) {
                createCustomerProfile(user, dto);
                break;
            } else if (role.getName().equals("ROLE_SHIPPER")) {
                createShipperProfile(user, dto);
                break;
            } else if (role.getName().equals("ROLE_WAREHOUSE_STAFF")) {
                createWarehouseProfile(user, dto);
                break;
            }
        }
        
        emailService.sendEmail(user.getEmail(), 
            "Tài khoản UTEExpress", 
            buildWelcomeEmail(user, dto.getPassword()));
        
        return user;
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
        
        customerRepository.findByUserId(id).ifPresent(customerRepository::delete);
        shipperRepository.findByUserId(id).ifPresent(shipperRepository::delete);
        warehouseRepository.findByUserId(id).ifPresent(warehouseRepository::delete);
        
        userRepository.delete(user);
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
        
        emailService.sendEmail(user.getEmail(),
            "Đặt lại mật khẩu UTEExpress",
            buildResetPasswordEmail(user, newPassword));
        
        return newPassword;
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
    
    private void createCustomerProfile(User user, AdminCreateUserDTO dto) {
        Customer customer = Customer.builder()
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .address(dto.getAddress())
            .user(user)
            .isEmailVerified(true)
            .status(CustomerStatus.ACTIVE)
            .build();
        customerRepository.save(customer);
    }
    
    private void createShipperProfile(User user, AdminCreateUserDTO dto) {
        Shipper shipper = Shipper.builder()
            .code("SHP" + System.currentTimeMillis())
            .name(user.getFullName())
            .phone(user.getPhone())
            .email(user.getEmail())
            .vehicleType(dto.getVehicleType())
            .vehicleNumber(dto.getVehicleNumber())
            .user(user)
            .isActive(true)
            .build();
        shipperRepository.save(shipper);
    }
    
    private void createWarehouseProfile(User user, AdminCreateUserDTO dto) {
        Warehouse warehouse = Warehouse.builder()
            .code("WH" + System.currentTimeMillis())
            .name(dto.getWarehouseName())
            .address(dto.getAddress())
            .phone(user.getPhone())
            .email(user.getEmail())
            .manager(user.getFullName())
            .totalCapacity(dto.getTotalCapacity() != null ? dto.getTotalCapacity() : 10000)
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