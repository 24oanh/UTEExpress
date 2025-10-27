// java/ltweb/controller/AdminChatController.java
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminChatController {
    
    private final ChatService chatService;
    private final UserService userService;

    @GetMapping
    public String chatPage(Model model) {
        List<ChatConversation> conversations = chatService.getActiveConversations();
        model.addAttribute("conversations", conversations);
        return "admin/chat";
    }

    @GetMapping("/conversation/{conversationId}")
    @ResponseBody
    public ResponseEntity<?> conversationDetail(@PathVariable Long conversationId) {
        try {
            ChatConversation conversation = chatService.getConversationById(conversationId);
            List<ChatMessage> messages = chatService.getMessages(conversationId);
            
            Map<String, Object> response = Map.of(
                "conversation", conversation,
                "messages", messages,
                "customer", conversation.getCustomer()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<ChatMessage> sendMessage(
            @RequestParam Long conversationId,
            @RequestParam String message,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) Long orderId) {
        
        try {
            User admin = userService.getCurrentUser();
            ChatMessage chatMessage = chatService.sendMessage(
                conversationId,
                ChatMessage.SenderType.SUPPORT,
                admin.getId(),
                admin.getFullName(),
                message,
                image,
                orderId
            );
            return ResponseEntity.ok(chatMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/conversation/{conversationId}/close")
    @ResponseBody
    public ResponseEntity<Void> closeConversation(@PathVariable Long conversationId) {
        chatService.closeConversation(conversationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversation/{conversationId}/mark-read")
    @ResponseBody
    public ResponseEntity<Void> markAsRead(@PathVariable Long conversationId) {
        chatService.markAsRead(conversationId, ChatMessage.SenderType.SUPPORT);
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/admin/chat/{conversationId}")
    @SendTo("/topic/admin/chat/{conversationId}")
    public ChatMessage handleAdminMessage(ChatMessage message) {
        return message;
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long unreadCount = chatService.getTotalUnreadForAdmin();
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }
}