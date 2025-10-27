package ltweb.service;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public Customer registerCustomer(CustomerRegistrationDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        if (customerRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }

        Role customerRole = roleRepository.findByName(RoleType.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));

        User user = User.builder()
                .username(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .isActive(true)
                .roles(new HashSet<>())
                .build();

        user.getRoles().add(customerRole);
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .user(user)
                .isEmailVerified(false)
                .status(CustomerStatus.ACTIVE)
                .build();

        customer = customerRepository.save(customer);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .type(VerificationType.EMAIL)
                .user(user)
                .build();

        verificationTokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(user.getEmail(), token);

        return customer;
    }

    @Transactional
    public boolean verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository
                .findByTokenAndType(token, VerificationType.EMAIL)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));

        if (verificationToken.getIsUsed()) {
            throw new RuntimeException("Token đã được sử dụng");
        }

        if (verificationToken.isExpired()) {
            throw new RuntimeException("Token đã hết hạn");
        }

        User user = verificationToken.getUser();
        user.setIsActive(true);
        userRepository.save(user);

        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
        customer.setIsEmailVerified(true);
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);

        verificationToken.setIsUsed(true);
        verificationTokenRepository.save(verificationToken);

        return true;
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        if (user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã được kích hoạt");
        }

        verificationTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .type(VerificationType.EMAIL)
                .user(user)
                .build();
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        String token = UUID.randomUUID().toString();
        VerificationToken resetToken = VerificationToken.builder()
                .token(token)
                .type(VerificationType.PASSWORD_RESET)
                .user(user)
                .build();
        verificationTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public boolean resetPassword(ResetPasswordDTO dto) {
        VerificationToken resetToken = verificationTokenRepository
                .findByTokenAndType(dto.getToken(), VerificationType.PASSWORD_RESET)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));

        if (resetToken.getIsUsed()) {
            throw new RuntimeException("Token đã được sử dụng");
        }

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token đã hết hạn");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        verificationTokenRepository.save(resetToken);

        return true;
    }

    public Customer getCustomerByUserId(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
    }

    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
    }

    @Transactional
    public Customer updateCustomer(Long customerId, Customer customerDetails) {
        Customer customer = getCustomerById(customerId);

        customer.setFullName(customerDetails.getFullName());
        customer.setPhone(customerDetails.getPhone());
        customer.setAddress(customerDetails.getAddress());

        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateProfile(Long customerId, UpdateProfileDTO dto) {
        Customer customer = getCustomerById(customerId);
        User user = customer.getUser();

        // Kiểm tra email mới có trùng không
        if (!user.getEmail().equals(dto.getEmail()) &&
                userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Kiểm tra phone mới có trùng không
        if (!customer.getPhone().equals(dto.getPhone()) &&
                customerRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }

        customer.setFullName(dto.getFullName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());

        userRepository.save(user);
        return customerRepository.save(customer);
    }

    @Transactional
    public boolean changePassword(Long customerId, ChangePasswordDTO dto) {
        Customer customer = getCustomerById(customerId);
        User user = customer.getUser();

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        return true;
    }

    @Transactional
    public Customer updateAvatar(Long customerId, MultipartFile avatarFile) {
        Customer customer = getCustomerById(customerId);

        try {
            if (customer.getAvatarUrl() != null && !customer.getAvatarUrl().isEmpty()) {
                cloudinaryService.deleteFileByUrl(customer.getAvatarUrl());
            }

            String avatarUrl = cloudinaryService.uploadFile(avatarFile);
            customer.setAvatarUrl(avatarUrl);

            return customerRepository.save(customer);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload avatar: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteAvatar(Long customerId) {
        Customer customer = getCustomerById(customerId);

        if (customer.getAvatarUrl() != null) {
            try {
                cloudinaryService.deleteFileByUrl(customer.getAvatarUrl());
            } catch (IOException e) {
                // Log error but continue
            }
            customer.setAvatarUrl(null);
            customerRepository.save(customer);
        }
    }
}