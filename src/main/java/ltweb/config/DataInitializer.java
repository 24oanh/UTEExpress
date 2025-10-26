package ltweb.config;

import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
	private final CustomerOrderRepository customerOrderRepository;

	// Thay thế toàn bộ method run

	@Override
	public void run(String... args) {

		if (userRepository.count() > 0 && shipperRepository.count() > 0 && routeRepository.count() > 0) {
			System.out.println("=== Data already exists, skipping initialization ===");
			return;
		}

		// Create roles
		Role warehouseRole = roleRepository.findByName(RoleType.ROLE_WAREHOUSE_STAFF)
				.orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_WAREHOUSE_STAFF).build()));
		Role shipperRole = roleRepository.findByName(RoleType.ROLE_SHIPPER)
				.orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_SHIPPER).build()));

		// ========== HANOI ==========
		User hnWarehouseUser = userRepository.findByEmail("warehouse.hn@uteexpress.com")
				.orElseGet(() -> userRepository.save(User.builder()
						.username("warehouse_hn")
						.password(passwordEncoder.encode("123456"))
						.email("warehouse.hn@uteexpress.com")
						.fullName("Warehouse Hanoi")
						.phone("0241234567")
						.isActive(true)
						.roles(Set.of(warehouseRole))
						.build()));

		Warehouse warehouseHN = warehouseRepository.findByCode("WH-HN")
				.orElseGet(() -> warehouseRepository.save(Warehouse.builder()
						.code("WH-HN")
						.name("Warehouse Hanoi")
						.address("No.1 Dai Co Viet, Hai Ba Trung, Hanoi")
						.phone("0241234567")
						.email("warehouse.hn@uteexpress.com")
						.manager("Nguyen Van A")
						.totalCapacity(15000)
						.currentStock(0)
						.user(hnWarehouseUser)
						.build()));

		User hnShipperUser = userRepository.findByEmail("shipper.hn@uteexpress.com")
				.orElseGet(() -> userRepository.save(User.builder()
						.username("shipper_hn")
						.password(passwordEncoder.encode("123456"))
						.email("shipper.hn@uteexpress.com")
						.fullName("Tran Van B")
						.phone("0981234567")
						.isActive(true)
						.roles(Set.of(shipperRole))
						.build()));

		Shipper shipperHN = shipperRepository.findByCode("SHP-HN")
				.orElseGet(() -> shipperRepository.save(Shipper.builder()
						.code("SHP-HN")
						.name("Tran Van B")
						.phone("0981234567")
						.email("shipper.hn@uteexpress.com")
						.vehicleType("Truck")
						.vehicleNumber("29A-12345")
						.isActive(true)
						.totalDeliveries(0)
						.successfulDeliveries(0)
						.failedDeliveries(0)
						.user(hnShipperUser)
						.build()));

		// ========== DANANG ==========
		User dnWarehouseUser = userRepository.findByEmail("warehouse.dn@uteexpress.com")
				.orElseGet(() -> userRepository.save(User.builder()
						.username("warehouse_dn")
						.password(passwordEncoder.encode("123456"))
						.email("warehouse.dn@uteexpress.com")
						.fullName("Warehouse Danang")
						.phone("0236234567")
						.isActive(true)
						.roles(Set.of(warehouseRole))
						.build()));

		Warehouse warehouseDN = warehouseRepository.findByCode("WH-DN")
				.orElseGet(() -> warehouseRepository.save(Warehouse.builder()
						.code("WH-DN")
						.name("Warehouse Danang")
						.address("No.54 Nguyen Luong Bang, Hoa Khanh, Danang")
						.phone("0236234567")
						.email("warehouse.dn@uteexpress.com")
						.manager("Le Thi C")
						.totalCapacity(12000)
						.currentStock(0)
						.user(dnWarehouseUser)
						.build()));

		User dnShipperUser = userRepository.findByEmail("shipper.dn@uteexpress.com")
				.orElseGet(() -> userRepository.save(User.builder()
						.username("shipper_dn")
						.password(passwordEncoder.encode("123456"))
						.email("shipper.dn@uteexpress.com")
						.fullName("Pham Van D")
						.phone("0982234567")
						.isActive(true)
						.roles(Set.of(shipperRole))
						.build()));

		Shipper shipperDN = shipperRepository.findByCode("SHP-DN")
				.orElseGet(() -> shipperRepository.save(Shipper.builder()
						.code("SHP-DN")
						.name("Pham Van D")
						.phone("0982234567")
						.email("shipper.dn@uteexpress.com")
						.vehicleType("Truck")
						.vehicleNumber("43A-23456")
						.isActive(true)
						.totalDeliveries(0)
						.successfulDeliveries(0)
						.failedDeliveries(0)
						.user(dnShipperUser)
						.build()));

		// ========== HCMC ==========
		User hcmWarehouseUser = userRepository.findByEmail("warehouse.hcm@uteexpress.com")
				.orElseGet(() -> userRepository.save(User.builder()
						.username("warehouse_hcm")
						.password(passwordEncoder.encode("123456"))
						.email("warehouse.hcm@uteexpress.com")
						.fullName("Warehouse HCMC")
						.phone("0283234567")
						.isActive(true)
						.roles(Set.of(warehouseRole))
						.build()));

		Warehouse warehouseHCM = warehouseRepository.findByCode("WH-HCM")
				.orElseGet(() -> warehouseRepository.save(Warehouse.builder()
						.code("WH-HCM")
						.name("Warehouse HCMC")
						.address("No.1 Vo Van Ngan, Thu Duc, HCMC")
						.phone("0283234567")
						.email("warehouse.hcm@uteexpress.com")
						.manager("Hoang Van E")
						.totalCapacity(20000)
						.currentStock(0)
						.user(hcmWarehouseUser)
						.build()));

		User hcmShipperUser = userRepository.findByEmail("shipper.hcm@uteexpress.com")
				.orElseGet(() -> userRepository.save(User.builder()
						.username("shipper_hcm")
						.password(passwordEncoder.encode("123456"))
						.email("shipper.hcm@uteexpress.com")
						.fullName("Vo Thi F")
						.phone("0983234567")
						.isActive(true)
						.roles(Set.of(shipperRole))
						.build()));

		Shipper shipperHCM = shipperRepository.findByCode("SHP-HCM")
				.orElseGet(() -> shipperRepository.save(Shipper.builder()
						.code("SHP-HCM")
						.name("Vo Thi F")
						.phone("0283234567")
						.email("shipper.hcm@uteexpress.com")
						.vehicleType("Truck")
						.vehicleNumber("59A-34567")
						.isActive(true)
						.totalDeliveries(0)
						.successfulDeliveries(0)
						.failedDeliveries(0)
						.user(hcmShipperUser)
						.build()));

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

		// Thay thế phần CUSTOMER ORDERS
		// ========== CUSTOMER ORDERS ==========
		CustomerOrder customerOrder1 = CustomerOrder.builder()
				.customerName("Nguyen Van Khach")
				.customerPhone("0901234567")
				.customerEmail("khach1@gmail.com")
				.senderName("Nguyen Van Khach")
				.senderPhone("0901234567")
				.senderAddress("123 Nguyen Trai, Thanh Xuan, Hanoi")
				.recipientName("Tran Thi Nhan")
				.recipientPhone("0912345678")
				.recipientAddress("456 Le Loi, Hai Chau, Danang")
				.estimatedFee(new BigDecimal("250000"))
				.packageDescription("Quan ao; Giay dep; Do dien tu")
				.totalWeight(15.5)
				.notes("Hang de vo, can xu ly can than")
				.fromWarehouseCode("WH-HN")
				.toWarehouseCode("WH-DN")
				.isProcessed(false)
				.build();
		customerOrderRepository.save(customerOrder1);

		CustomerOrder customerOrder2 = CustomerOrder.builder()
				.customerName("Le Thi Mai")
				.customerPhone("0923456789")
				.customerEmail("mai.le@gmail.com")
				.senderName("Le Thi Mai")
				.senderPhone("0923456789")
				.senderAddress("789 Tran Hung Dao, District 1, HCMC")
				.recipientName("Pham Van Binh")
				.recipientPhone("0934567890")
				.recipientAddress("321 Ba Trieu, Hoan Kiem, Hanoi")
				.estimatedFee(new BigDecimal("350000"))
				.packageDescription("Sach vo; Do choi tre em")
				.totalWeight(8.0)
				.notes("Giao hang truoc 5h chieu")
				.fromWarehouseCode("WH-HCM")
				.toWarehouseCode("WH-HN")
				.isProcessed(false)
				.build();
		customerOrderRepository.save(customerOrder2);

		CustomerOrder customerOrder3 = CustomerOrder.builder()
				.customerName("Hoang Van Minh")
				.customerPhone("0945678901")
				.customerEmail("minh.hoang@yahoo.com")
				.senderName("Hoang Van Minh")
				.senderPhone("0945678901")
				.senderAddress("555 Hai Ba Trung, Son Tra, Danang")
				.recipientName("Vo Thi Lan")
				.recipientPhone("0956789012")
				.recipientAddress("777 Nguyen Hue, District 1, HCMC")
				.estimatedFee(new BigDecimal("300000"))
				.packageDescription("Thuc pham dong goi; Do uong")
				.totalWeight(20.0)
				.notes("Can giu lanh")
				.fromWarehouseCode("WH-DN")
				.toWarehouseCode("WH-HCM")
				.isProcessed(false)
				.build();
		customerOrderRepository.save(customerOrder3);

		CustomerOrder customerOrder4 = CustomerOrder.builder()
				.customerName("Tran Van Duc")
				.customerPhone("0967890123")
				.customerEmail("duc.tran@gmail.com")
				.senderName("Tran Van Duc")
				.senderPhone("0967890123")
				.senderAddress("100 Le Duan, Dong Da, Hanoi")
				.recipientName("Nguyen Thi Hong")
				.recipientPhone("0978901234")
				.recipientAddress("200 Tran Phu, Thanh Khe, Danang")
				.estimatedFee(new BigDecimal("280000"))
				.packageDescription("May tinh xach tay; Phu kien")
				.totalWeight(5.5)
				.notes("Hang gia tri cao, can bao quan ky")
				.fromWarehouseCode("WH-HN")
				.toWarehouseCode("WH-DN")
				.isProcessed(false)
				.build();
		customerOrderRepository.save(customerOrder4);

		CustomerOrder customerOrder5 = CustomerOrder.builder()
				.customerName("Pham Thi Nga")
				.customerPhone("0989012345")
				.customerEmail("nga.pham@yahoo.com")
				.senderName("Pham Thi Nga")
				.senderPhone("0989012345")
				.senderAddress("300 Nguyen Thi Minh Khai, District 3, HCMC")
				.recipientName("Le Van Thanh")
				.recipientPhone("0990123456")
				.recipientAddress("400 Ly Thuong Kiet, Hai Ba Trung, Hanoi")
				.estimatedFee(new BigDecimal("400000"))
				.packageDescription("Noi that; Do trang tri")
				.totalWeight(35.0)
				.notes("Hang cong kenh, can 2 nguoi khuyen")
				.fromWarehouseCode("WH-HCM")
				.toWarehouseCode("WH-HN")
				.isProcessed(false)
				.build();
		customerOrderRepository.save(customerOrder5);

		System.out.println("Customer Orders: 5 orders created (HN: 2, HCM: 2, DN: 1)");

		System.out.println("Customer Orders: 3 orders created");
		// KHÔNG có route trực tiếp HN <-> HCM
		// Phải qua DN (được xử lý trong RouteCalculationService)
		System.out.println("=== Initial Data Created ===");
		System.out.println("Warehouses: warehouse_hn, warehouse_dn, warehouse_hcm (password: 123456)");
		System.out.println("Shippers: shipper_hn, shipper_dn, shipper_hcm (password: 123456)");

	}
}