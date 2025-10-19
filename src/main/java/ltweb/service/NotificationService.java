package ltweb.service;

import ltweb.entity.Notification;
import ltweb.entity.NotificationType;
import ltweb.entity.Order;
import ltweb.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
    }

    public List<Notification> getNotificationsByRecipient(String recipientType, Long recipientId) {
        return notificationRepository.findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, recipientId);
    }

    public List<Notification> getUnreadNotifications(String recipientType, Long recipientId) {
        return notificationRepository.findByRecipientTypeAndRecipientIdAndIsRead(recipientType, recipientId, false);
    }

    public long countUnreadNotifications(String recipientType, Long recipientId) {
        return notificationRepository.countByRecipientTypeAndRecipientIdAndIsRead(recipientType, recipientId, false);
    }

    @Transactional
    public Notification createNotification(String recipientType, Long recipientId, 
                                          String message, NotificationType type, Order order) {
        Notification notification = Notification.builder()
                .recipientType(recipientType)
                .recipientId(recipientId)
                .message(message)
                .type(type)
                .order(order)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        
        sendRealtimeNotification(recipientType, recipientId, savedNotification);
        
        return savedNotification;
    }

    @Transactional
    public Notification markAsRead(Long id) {
        Notification notification = getNotificationById(id);
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String recipientType, Long recipientId) {
        List<Notification> unreadNotifications = getUnreadNotifications(recipientType, recipientId);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    private void sendRealtimeNotification(String recipientType, Long recipientId, Notification notification) {
        String destination = "/topic/notifications/" + recipientType.toLowerCase() + "/" + recipientId;
        messagingTemplate.convertAndSend(destination, notification);
    }

    public void sendCustomNotification(String recipientType, Long recipientId, String message) {
        String destination = "/topic/notifications/" + recipientType.toLowerCase() + "/" + recipientId;
        messagingTemplate.convertAndSend(destination, message);
    }
}