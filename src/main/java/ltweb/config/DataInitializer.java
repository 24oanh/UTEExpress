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
	private final RouteRepository routeRepository;

	// Thay thế toàn bộ method run

	@Override
	public void run(String... args) {

		// Create roles
		Role warehouseRole = roleRepository.save(Role.builder().name(RoleType.ROLE_WAREHOUSE_STAFF).build());
		Role shipperRole = roleRepository.save(Role.builder().name(RoleType.ROLE_SHIPPER).build());

		// ========== HANOI ==========
		User hnWarehouseUser = User.builder().username("warehouse_hn").password(passwordEncoder.encode("123456"))
				.email("warehouse.hn@uteexpress.com").fullName("Warehouse Hanoi").phone("0241234567").isActive(true)
				.roles(Set.of(warehouseRole)).build();
		hnWarehouseUser = userRepository.save(hnWarehouseUser);

		Warehouse warehouseHN = Warehouse.builder().code("WH-HN").name("Warehouse Hanoi")
				.address("No.1 Dai Co Viet, Hai Ba Trung, Hanoi").phone("0241234567")
				.email("warehouse.hn@uteexpress.com").manager("Nguyen Van A").totalCapacity(15000).currentStock(0)
				.user(hnWarehouseUser).build();
		warehouseHN = warehouseRepository.save(warehouseHN);

		User hnShipperUser = User.builder().username("shipper_hn").password(passwordEncoder.encode("123456"))
				.email("shipper.hn@uteexpress.com").fullName("Tran Van B").phone("0981234567").isActive(true)
				.roles(Set.of(shipperRole)).build();
		hnShipperUser = userRepository.save(hnShipperUser);

		Shipper shipperHN = Shipper.builder().code("SHP-HN").name("Tran Van B").phone("0981234567")
				.email("shipper.hn@uteexpress.com").vehicleType("Truck").vehicleNumber("29A-12345").isActive(true)
				.totalDeliveries(0).successfulDeliveries(0).failedDeliveries(0).user(hnShipperUser).build();
		shipperHN = shipperRepository.save(shipperHN);

		// ========== DANANG ==========
		User dnWarehouseUser = User.builder().username("warehouse_dn").password(passwordEncoder.encode("123456"))
				.email("warehouse.dn@uteexpress.com").fullName("Warehouse Danang").phone("0236234567").isActive(true)
				.roles(Set.of(warehouseRole)).build();
		dnWarehouseUser = userRepository.save(dnWarehouseUser);

		Warehouse warehouseDN = Warehouse.builder().code("WH-DN").name("Warehouse Danang")
				.address("No.54 Nguyen Luong Bang, Hoa Khanh, Danang").phone("0236234567")
				.email("warehouse.dn@uteexpress.com").manager("Le Thi C").totalCapacity(12000).currentStock(0)
				.user(dnWarehouseUser).build();
		warehouseDN = warehouseRepository.save(warehouseDN);

		User dnShipperUser = User.builder().username("shipper_dn").password(passwordEncoder.encode("123456"))
				.email("shipper.dn@uteexpress.com").fullName("Pham Van D").phone("0982234567").isActive(true)
				.roles(Set.of(shipperRole)).build();
		dnShipperUser = userRepository.save(dnShipperUser);

		Shipper shipperDN = Shipper.builder().code("SHP-DN").name("Pham Van D").phone("0982234567")
				.email("shipper.dn@uteexpress.com").vehicleType("Truck").vehicleNumber("43A-23456").isActive(true)
				.totalDeliveries(0).successfulDeliveries(0).failedDeliveries(0).user(dnShipperUser).build();
		shipperDN = shipperRepository.save(shipperDN);

		// ========== HCMC ==========
		User hcmWarehouseUser = User.builder().username("warehouse_hcm").password(passwordEncoder.encode("123456"))
				.email("warehouse.hcm@uteexpress.com").fullName("Warehouse HCMC").phone("0283234567").isActive(true)
				.roles(Set.of(warehouseRole)).build();
		hcmWarehouseUser = userRepository.save(hcmWarehouseUser);

		Warehouse warehouseHCM = Warehouse.builder().code("WH-HCM").name("Warehouse HCMC")
				.address("No.1 Vo Van Ngan, Thu Duc, HCMC").phone("0283234567").email("warehouse.hcm@uteexpress.com")
				.manager("Hoang Van E").totalCapacity(20000).currentStock(0).user(hcmWarehouseUser).build();
		warehouseHCM = warehouseRepository.save(warehouseHCM);

		User hcmShipperUser = User.builder().username("shipper_hcm").password(passwordEncoder.encode("123456"))
				.email("shipper.hcm@uteexpress.com").fullName("Vo Thi F").phone("0983234567").isActive(true)
				.roles(Set.of(shipperRole)).build();
		hcmShipperUser = userRepository.save(hcmShipperUser);

		Shipper shipperHCM = Shipper.builder().code("SHP-HCM").name("Vo Thi F").phone("0983234567")
				.email("shipper.hcm@uteexpress.com").vehicleType("Truck").vehicleNumber("59A-34567").isActive(true)
				.totalDeliveries(0).successfulDeliveries(0).failedDeliveries(0).user(hcmShipperUser).build();
		shipperHCM = shipperRepository.save(shipperHCM);

		// ========== ROUTES ==========
		// HN -> DN (trực tiếp)
		routeRepository.save(Route.builder()
				.fromWarehouse(warehouseHN)
				.toWarehouse(warehouseDN)
				.preferredShipper(shipperHN)
				.distanceKm(764.0)
				.estimatedHours(12.0)
				.isActive(true)
				.build());

		// DN -> HN (trực tiếp)
		routeRepository.save(Route.builder()
				.fromWarehouse(warehouseDN)
				.toWarehouse(warehouseHN)
				.preferredShipper(shipperDN)
				.distanceKm(764.0)
				.estimatedHours(12.0)
				.isActive(true)
				.build());

		// HCM -> DN (trực tiếp)
		routeRepository.save(Route.builder()
				.fromWarehouse(warehouseHCM)
				.toWarehouse(warehouseDN)
				.preferredShipper(shipperHCM)
				.distanceKm(964.0)
				.estimatedHours(14.0)
				.isActive(true)
				.build());

		// DN -> HCM (trực tiếp)
		routeRepository.save(Route.builder()
				.fromWarehouse(warehouseDN)
				.toWarehouse(warehouseHCM)
				.preferredShipper(shipperDN)
				.distanceKm(964.0)
				.estimatedHours(14.0)
				.isActive(true)
				.build());

		// KHÔNG có route trực tiếp HN <-> HCM
		// Phải qua DN (được xử lý trong RouteCalculationService)
		System.out.println("=== Initial Data Created ===");
		System.out.println("Warehouses: warehouse_hn, warehouse_dn, warehouse_hcm (password: 123456)");
		System.out.println("Shippers: shipper_hn, shipper_dn, shipper_hcm (password: 123456)");
	}
}