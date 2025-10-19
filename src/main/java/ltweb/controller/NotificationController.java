package ltweb.controller;

import ltweb.entity.Notification;
import ltweb.entity.Shipper;
import ltweb.entity.Warehouse;
import ltweb.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/warehouse/notifications")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    public String warehouseNotifications(Model model, HttpSession session) {
        Warehouse warehouse = (Warehouse) session.getAttribute("currentWarehouse");
        List<Notification> notifications = notificationService.getNotificationsByRecipient("WAREHOUSE", warehouse.getId());
        long unreadCount = notificationService.countUnreadNotifications("WAREHOUSE", warehouse.getId());
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "warehouse/notifications";
    }

    @GetMapping("/shipper/notifications")
    @PreAuthorize("hasRole('SHIPPER')")
    public String shipperNotifications(Model model, HttpSession session) {
        Shipper shipper = (Shipper) session.getAttribute("currentShipper");
        List<Notification> notifications = notificationService.getNotificationsByRecipient("SHIPPER", shipper.getId());
        long unreadCount = notificationService.countUnreadNotifications("SHIPPER", shipper.getId());
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "shipper/notifications";
    }

    @GetMapping("/api/notifications/{recipientType}/{recipientId}")
    @ResponseBody
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable String recipientType,
                                                               @PathVariable Long recipientId) {
        try {
            List<Notification> notifications = notificationService.getNotificationsByRecipient(recipientType, recipientId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/notifications/{recipientType}/{recipientId}/unread")
    @ResponseBody
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable String recipientType,
                                                                     @PathVariable Long recipientId) {
        try {
            List<Notification> notifications = notificationService.getUnreadNotifications(recipientType, recipientId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/notifications/{recipientType}/{recipientId}/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String recipientType,
                                                            @PathVariable Long recipientId) {
        try {
            long count = notificationService.countUnreadNotifications(recipientType, recipientId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        try {
            Notification notification = notificationService.markAsRead(id);
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/notifications/{recipientType}/{recipientId}/read-all")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable String recipientType,
                                                             @PathVariable Long recipientId) {
        try {
            notificationService.markAllAsRead(recipientType, recipientId);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/api/notifications/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @MessageMapping("/notifications/send")
    @SendTo("/topic/notifications")
    public Notification sendNotification(Map<String, Object> notificationData) {
        String recipientType = (String) notificationData.get("recipientType");
        Long recipientId = ((Number) notificationData.get("recipientId")).longValue();
        String message = (String) notificationData.get("message");
        
        notificationService.sendCustomNotification(recipientType, recipientId, message);
        
        return null;
    }
}