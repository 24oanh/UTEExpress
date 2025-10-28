package ltweb.service;

import ltweb.dto.RegisterDTO;
import ltweb.entity.*;
import ltweb.entity.User;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User account is disabled");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                role.getName().name()))
                        .toArray(org.springframework.security.core.GrantedAuthority[]::new))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User register(RegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Tạo User trước
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .isActive(true)
                .roles(new HashSet<>())
                .build();

        // Lấy role CUSTOMER
        Role customerRole = roleRepository.findByName(RoleType.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Customer role not found"));

        user.getRoles().add(customerRole);

        // Lưu User trước
        user = userRepository.save(user);

        // Sau đó tạo Customer với User đã có ID
        Customer customer = Customer.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .user(user)
                .isEmailVerified(true)
                .status(CustomerStatus.ACTIVE)
                .build();

        customerRepository.save(customer);

        return user;
    }
}