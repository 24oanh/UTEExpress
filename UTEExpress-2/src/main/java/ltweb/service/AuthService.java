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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ShipperRepository shipperRepository;
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
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName().name()))
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

        User user = User.builder()
            .username(dto.getUsername())
            .password(passwordEncoder.encode(dto.getPassword()))
            .email(dto.getEmail())
            .fullName(dto.getFullName())
            .phone(dto.getPhone())
            .isActive(true)
            .build();

        switch (dto.getAccountType().toUpperCase()) {
            case "ADMIN":
                Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
                user.setRoles(Set.of(adminRole));
                break;

            case "WAREHOUSE_STAFF":
                Role warehouseRole = roleRepository.findByName(RoleType.ROLE_WAREHOUSE_STAFF)
                    .orElseThrow(() -> new RuntimeException("Warehouse role not found"));
                user.setRoles(Set.of(warehouseRole));
                user = userRepository.save(user);

                Warehouse warehouse = Warehouse.builder()
                    .code("WH-" + System.currentTimeMillis())
                    .name(dto.getWarehouseName())
                    .address(dto.getWarehouseAddress())
                    .phone(dto.getPhone())
                    .email(dto.getEmail())
                    .manager(dto.getFullName())
                    .totalCapacity(dto.getTotalCapacity() != null ? dto.getTotalCapacity() : 10000)
                    .currentStock(0)
                    .user(user)
                    .build();
                warehouseRepository.save(warehouse);
                break;

            case "SHIPPER":
                Role shipperRole = roleRepository.findByName(RoleType.ROLE_SHIPPER)
                    .orElseThrow(() -> new RuntimeException("Shipper role not found"));
                user.setRoles(Set.of(shipperRole));
                user = userRepository.save(user);

                Shipper shipper = Shipper.builder()
                    .code("SHP-" + System.currentTimeMillis())
                    .name(dto.getFullName())
                    .phone(dto.getPhone())
                    .email(dto.getEmail())
                    .vehicleType(dto.getVehicleType())
                    .vehicleNumber(dto.getVehicleNumber())
                    .isActive(true)
                    .totalDeliveries(0)
                    .successfulDeliveries(0)
                    .failedDeliveries(0)
                    .user(user)
                    .build();
                shipperRepository.save(shipper);
                break;

            case "CUSTOMER":
            default:
                Role customerRole = roleRepository.findByName(RoleType.ROLE_CUSTOMER)
                    .orElseThrow(() -> new RuntimeException("Customer role not found"));
                user.setRoles(Set.of(customerRole));
                user = userRepository.save(user);

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
                break;
        }

        return userRepository.save(user);
    }
}