// java/ltweb/controller/AdminNotificationController.java
package ltweb.controller;

import ltweb.entity.*;
import ltweb.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {
    
    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String notificationsPage(Model model) {
        User admin = userService.getCurrentUser();
        List<Notification> notifications = notificationService.getNotificationsByUserId(admin.getId());
        model.addAttribute("notifications", notifications);
        return "admin/notifications";
    }

    @GetMapping("/unread")
    @ResponseBody
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        User admin = userService.getCurrentUser();
        List<Notification> notifications = notificationService.getUnreadNotifications(admin.getId());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User admin = userService.getCurrentUser();
        long count = notificationService.countUnreadNotifications(admin.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/{id}/mark-read")
    @ResponseBody
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    @ResponseBody
    public ResponseEntity<Void> markAllAsRead() {
        User admin = userService.getCurrentUser();
        notificationService.markAllAsRead(admin.getId());
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/admin/notifications")
    @SendTo("/topic/admin/notifications")
    public Notification handleNotification(Notification notification) {
        return notification;
    }
}