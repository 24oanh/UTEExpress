package ltweb.config;

import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WarehouseRepository warehouseRepository;
    private final ShipperRepository shipperRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // Create roles
        Role warehouseRole = roleRepository.save(Role.builder()
                .name(RoleType.ROLE_WAREHOUSE_STAFF)
                .build());

        Role shipperRole = roleRepository.save(Role.builder()
                .name(RoleType.ROLE_SHIPPER)
                .build());

        // Create warehouse user
        User warehouseUser = User.builder()
                .username("warehouse")
                .password(passwordEncoder.encode("123456"))
                .email("warehouse@uteexpress.com")
                .fullName("Warehouse Staff")
                .phone("0123456789")
                .isActive(true)
                .roles(Set.of(warehouseRole))
                .build();
        warehouseUser = userRepository.save(warehouseUser);

        // Create warehouse
        Warehouse warehouse = Warehouse.builder()
                .code("WH001")
                .name("Main Warehouse")
                .address("123 Main Street, District 1, HCMC")
                .phone("0123456789")
                .email("warehouse@uteexpress.com")
                .manager("Warehouse Staff")
                .totalCapacity(10000)
                .currentStock(0)
                .user(warehouseUser)
                .build();
        warehouseRepository.save(warehouse);

        // Create shipper user
        User shipperUser = User.builder()
                .username("shipper")
                .password(passwordEncoder.encode("123456"))
                .email("shipper@uteexpress.com")
                .fullName("Shipper One")
                .phone("0987654321")
                .isActive(true)
                .roles(Set.of(shipperRole))
                .build();
        shipperUser = userRepository.save(shipperUser);

        // Create shipper
        Shipper shipper = Shipper.builder()
                .code("SHP001")
                .name("Shipper One")
                .phone("0987654321")
                .email("shipper@uteexpress.com")
                .vehicleType("Motorbike")
                .vehicleNumber("59A-12345")
                .isActive(true)
                .totalDeliveries(0)
                .successfulDeliveries(0)
                .failedDeliveries(0)
                .user(shipperUser)
                .build();
        shipperRepository.save(shipper);

        System.out.println("=== Initial Data Created ===");
        System.out.println("Warehouse: username=warehouse, password=123456");
        System.out.println("Shipper: username=shipper, password=123456");
    }
}