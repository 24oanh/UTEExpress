// ltweb2/service/CustomerOrderService.java
package ltweb.service;

import ltweb.dto.*;
import ltweb.entity.*;
import ltweb.entity.Package;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final OrderRepository orderRepository;
    private final PackageRepository packageRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final ShippingFeeService shippingFeeService;
    private final NotificationService notificationService;

    public OrderSummaryDTO calculateOrderSummary(CreateOrderDTO dto) {
        Warehouse destinationWarehouse = warehouseRepository.findById(dto.getDestinationWarehouseId())
                .orElseThrow(() -> new RuntimeException("Kho đích không tồn tại"));

        Double distance = calculateDistanceFromAddress(dto.getSenderAddress(), dto.getRecipientAddress());

        BigDecimal estimatedFee = shippingFeeService.calculateShippingFee(distance, dto.getWeight(), dto.getServiceType());

        int deliveryDays = shippingFeeService.getEstimatedDeliveryDays(distance, dto.getServiceType());

        LocalDateTime estimatedDeliveryDate = LocalDateTime.now().plusDays(deliveryDays);

        return OrderSummaryDTO.builder()
                .senderName(dto.getSenderName())
                .senderPhone(dto.getSenderPhone())
                .senderAddress(dto.getSenderAddress())
                .recipientName(dto.getRecipientName())
                .recipientPhone(dto.getRecipientPhone())
                .recipientAddress(dto.getRecipientAddress())
                .serviceType(dto.getServiceType())
                .notes(dto.getNotes())
                .itemDescription(dto.getItemDescription())
                .weight(dto.getWeight())
                .length(dto.getLength())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .quantity(dto.getQuantity())
                .distance(distance)
                .estimatedFee(estimatedFee)
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .destinationWarehouseName(destinationWarehouse.getName())
                .build();
    }

    private Double calculateDistanceFromAddress(String fromAddress, String toAddress) {
        String from = fromAddress.toLowerCase();
        String to = toAddress.toLowerCase();

        if ((from.contains("hà nội") && to.contains("đà nẵng")) ||
            (from.contains("đà nẵng") && to.contains("hà nội"))) {
            return 764.0;
        } else if ((from.contains("hà nội") && (to.contains("hồ chí minh") || to.contains("hcm"))) ||
                   ((from.contains("hồ chí minh") || from.contains("hcm")) && to.contains("hà nội"))) {
            return 1720.0;
        } else if ((from.contains("đà nẵng") && (to.contains("hồ chí minh") || to.contains("hcm"))) ||
                   ((from.contains("hồ chí minh") || from.contains("hcm")) && to.contains("đà nẵng"))) {
            return 964.0;
        }

        return 100.0;
    }

    @Transactional
    public Order createOrder(CreateOrderDTO dto, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));

        Warehouse nearestWarehouse = findNearestWarehouse(dto.getSenderAddress());

        Warehouse destinationWarehouse = warehouseRepository.findById(dto.getDestinationWarehouseId())
                .orElseThrow(() -> new RuntimeException("Kho đích không tồn tại"));

        Double distance = calculateDistance(dto.getSenderAddress(), dto.getRecipientAddress());

        BigDecimal shippingFee = shippingFeeService.calculateShippingFee(distance, dto.getWeight(), dto.getServiceType());

        int deliveryDays = shippingFeeService.getEstimatedDeliveryDays(distance, dto.getServiceType());

        LocalDateTime estimatedDeliveryDate = LocalDateTime.now().plusDays(deliveryDays);

        Order order = Order.builder()
                .orderCode("ORD" + System.currentTimeMillis())
                .senderName(dto.getSenderName())
                .senderPhone(dto.getSenderPhone())
                .senderAddress(dto.getSenderAddress())
                .recipientName(dto.getRecipientName())
                .recipientPhone(dto.getRecipientPhone())
                .recipientAddress(dto.getRecipientAddress())
                .shipmentFee(shippingFee)
                .notes(dto.getNotes())
                .status(OrderStatus.CHO_GIAO)
                .warehouse(nearestWarehouse)
                .destinationWarehouse(destinationWarehouse)
                .customer(customer)
                .serviceType(dto.getServiceType())
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .totalWeight(dto.getWeight())
                .totalDistance(distance)
                .build();

        order = orderRepository.save(order);

        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        Package packageItem = Package.builder()
                .packageCode("PKG" + System.currentTimeMillis())
                .order(order)
                .description(dto.getItemDescription())
                .weight(dto.getWeight())
                .length(dto.getLength())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .unitQuantity(dto.getQuantity())
                .status(PackageStatus.KHO)
                .build();

        packageRepository.save(packageItem);

        notificationService.notifyWarehouseForNewOrder(order);

        return order;
    }

    private Warehouse findNearestWarehouse(String address) {
        List<Warehouse> warehouses = warehouseRepository.findAll();

        if (address.toLowerCase().contains("hà nội") || address.toLowerCase().contains("hanoi")) {
            return warehouses.stream()
                    .filter(w -> w.getCode().equals("WH-HN"))
                    .findFirst()
                    .orElse(warehouses.get(0));
        } else if (address.toLowerCase().contains("đà nẵng") || address.toLowerCase().contains("danang")) {
            return warehouses.stream()
                    .filter(w -> w.getCode().equals("WH-DN"))
                    .findFirst()
                    .orElse(warehouses.get(0));
        } else if (address.toLowerCase().contains("hồ chí minh") ||
                   address.toLowerCase().contains("hcm") ||
                   address.toLowerCase().contains("sài gòn")) {
            return warehouses.stream()
                    .filter(w -> w.getCode().equals("WH-HCM"))
                    .findFirst()
                    .orElse(warehouses.get(0));
        }

        return warehouses.get(0);
    }

    private Double calculateDistance(String fromAddress, String toAddress) {
        String from = fromAddress.toLowerCase();
        String to = toAddress.toLowerCase();

        if ((from.contains("hà nội") && to.contains("đà nẵng")) ||
            (from.contains("đà nẵng") && to.contains("hà nội"))) {
            return 764.0;
        } else if ((from.contains("hà nội") && to.contains("hồ chí minh")) ||
                   (from.contains("hồ chí minh") && to.contains("hà nội"))) {
            return 1720.0;
        } else if ((from.contains("đà nẵng") && to.contains("hồ chí minh")) ||
                   (from.contains("hồ chí minh") && to.contains("đà nẵng"))) {
            return 964.0;
        }

        return 100.0;
    }

    public List<Order> getCustomerOrders(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
    }
}