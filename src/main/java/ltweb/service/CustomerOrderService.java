package ltweb.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ltweb.entity.CustomerOrder;
import ltweb.entity.NotificationType;
import ltweb.entity.Order;
import ltweb.entity.OrderStatus;
import ltweb.entity.PackageConfirmation;
import ltweb.entity.Warehouse;
import ltweb.repository.CustomerOrderRepository;
import ltweb.repository.OrderRepository;
import ltweb.repository.PackageConfirmationRepository;
import ltweb.repository.WarehouseRepository;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {
    private final CustomerOrderRepository customerOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final PackageConfirmationRepository packageConfirmationRepository;

    @Transactional
    public CustomerOrder createCustomerOrder(CustomerOrder customerOrder) {
        return customerOrderRepository.save(customerOrder);
    }

    public List<CustomerOrder> getPendingOrders() {
        return customerOrderRepository.findByIsProcessedFalseOrderByCreatedAtDesc();
    }

    public List<CustomerOrder> getPendingOrdersByWarehouse(String warehouseCode) {
        return customerOrderRepository.findByFromWarehouseCodeAndIsProcessedFalseOrderByCreatedAtDesc(warehouseCode);
    }

    public CustomerOrder getCustomerOrderById(Long id) {
        return customerOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer order not found"));
    }

    @Transactional
    public void markAsProcessed(Long id, Long orderId) {
        CustomerOrder customerOrder = getCustomerOrderById(id);
        customerOrder.setIsProcessed(true);
        customerOrder.setProcessedOrderId(orderId);
        customerOrderRepository.save(customerOrder);
    }

    // CustomerOrderService.java - Đảm bảo set status rõ ràng
    @Transactional
    public Order acceptSingleOrder(Long id) {
        CustomerOrder customerOrder = customerOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer order not found"));

        if (customerOrder.getIsProcessed()) {
            throw new RuntimeException("Đơn hàng đã được xử lý");
        }

        Warehouse fromWarehouse = warehouseRepository.findByCode(customerOrder.getFromWarehouseCode())
                .orElseThrow(() -> new RuntimeException("From warehouse not found"));
        Warehouse toWarehouse = warehouseRepository.findByCode(customerOrder.getToWarehouseCode())
                .orElseThrow(() -> new RuntimeException("To warehouse not found"));

        Order order = new Order();
        order.setOrderCode("ORD" + System.currentTimeMillis());
        order.setSenderName(customerOrder.getSenderName());
        order.setSenderPhone(customerOrder.getSenderPhone());
        order.setSenderAddress(customerOrder.getSenderAddress());
        order.setRecipientName(customerOrder.getRecipientName());
        order.setRecipientPhone(customerOrder.getRecipientPhone());
        order.setRecipientAddress(customerOrder.getRecipientAddress());
        order.setShipmentFee(customerOrder.getEstimatedFee());
        order.setNotes(customerOrder.getNotes());
        order.setWarehouse(fromWarehouse);
        order.setDestinationWarehouse(toWarehouse);
        order.setStatus(OrderStatus.CHO_XAC_NHAN);
        order.setIsConfirmed(false);

        Order savedOrder = orderRepository.save(order);

        String[] descriptions = customerOrder.getPackageDescription().split(";");
        for (int i = 0; i < descriptions.length; i++) {
            PackageConfirmation confirmation = new PackageConfirmation();
            confirmation.setOrder(savedOrder);
            confirmation.setPackageCode("PKG" + savedOrder.getId() + "-" + (i + 1));
            confirmation.setDescription(descriptions[i].trim());
            confirmation.setWeight(customerOrder.getTotalWeight() / descriptions.length);
            confirmation.setLength(0.0);
            confirmation.setWidth(0.0);
            confirmation.setHeight(0.0);
            confirmation.setUnitQuantity(1);
            confirmation.setIsConfirmed(false);
            packageConfirmationRepository.save(confirmation);
        }

        customerOrder.setIsProcessed(true);
        customerOrder.setProcessedOrderId(savedOrder.getId());
        customerOrderRepository.save(customerOrder);

        notificationService.createNotification("WAREHOUSE", fromWarehouse.getId(),
                "Đơn hàng mới từ khách: " + savedOrder.getOrderCode(),
                NotificationType.ORDER_CREATED, savedOrder);

        return savedOrder;
    }
}