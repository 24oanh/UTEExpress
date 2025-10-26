package ltweb.service;

import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createNotification(Long userId, String recipientType,
                                         String message, NotificationType type, Order order) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .recipientType(recipientType)
                .message(message)
                .type(type)
                .order(order)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        messagingTemplate.convertAndSend("/topic/notifications/" + recipientType + "/" + userId, notification);

        return notification;
    }

    @Transactional
    public Notification createCustomerNotification(Long customerId, String message,
                                                  NotificationType type, Order order) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .recipientType("CUSTOMER")
                .message(message)
                .type(type)
                .order(order)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        messagingTemplate.convertAndSend("/topic/notifications/CUSTOMER/" + customerId, notification);

        return notification;
    }

    @Transactional
    public void notifyWarehouseForNewOrder(Order order) {
        if (order.getWarehouse() != null && order.getWarehouse().getUser() != null) {
            User warehouseUser = order.getWarehouse().getUser();
            
            String message = "Đơn hàng mới: " + order.getOrderCode() + 
                           " từ khách hàng " + order.getCustomer().getFullName();

            Notification notification = Notification.builder()
                    .user(warehouseUser)
                    .recipientType("WAREHOUSE")
                    .message(message)
                    .type(NotificationType.ORDER_CREATED)
                    .order(order)
                    .isRead(false)
                    .build();

            notification = notificationRepository.save(notification);

            messagingTemplate.convertAndSend("/topic/notifications/WAREHOUSE/" + warehouseUser.getId(), notification);
        }
    }

    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getNotificationsByRecipient(String recipientType, Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
    }

    public List<Notification> getUnreadNotifications(String recipientType, Long userId) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
    }

    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    public long countUnreadNotifications(String recipientType, Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = getUnreadNotifications(userId);
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markAllAsRead(String recipientType, Long userId) {
        markAllAsRead(userId);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void sendCustomNotification(String recipientType, Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .recipientType(recipientType)
                .message(message)
                .type(NotificationType.SYSTEM_ALERT)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        messagingTemplate.convertAndSend("/topic/notifications/" + recipientType + "/" + userId, notification);
    }

    public List<Notification> getAllNotificationsForAdmin() {
        List<User> admins = userRepository.findByRolesName(RoleType.ROLE_ADMIN);

        if (admins.isEmpty()) {
            return List.of();
        }

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(admins.get(0).getId());
    }

    public long countUnreadForAllAdmins() {
        List<User> admins = userRepository.findByRolesName(RoleType.ROLE_ADMIN);

        return admins.stream()
                .mapToLong(admin -> notificationRepository.countByUserIdAndIsRead(admin.getId(), false))
                .sum();
    }
}