package ltweb.repository;

import ltweb.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByRecipientTypeAndRecipientId(String recipientType, Long recipientId);
    
    List<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(String recipientType, Long recipientId);
    
    List<Notification> findByRecipientTypeAndRecipientIdAndIsRead(String recipientType, Long recipientId, Boolean isRead);
    
    long countByRecipientTypeAndRecipientIdAndIsRead(String recipientType, Long recipientId, Boolean isRead);
}