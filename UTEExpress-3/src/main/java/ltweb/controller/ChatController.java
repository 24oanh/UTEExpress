package ltweb.controller;

import ltweb.entity.*;
import ltweb.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/customer/chat")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerChat(Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("currentCustomer");
        ChatConversation conversation = chatService.getOrCreateConversation(customer.getId());
        List<ChatMessage> messages = chatService.getMessages(conversation.getId());
        
        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", messages);
        model.addAttribute("conversations", new ArrayList<>()); // ADD THIS LINE
        
        return "customer/chat";
    }

    @PostMapping("/customer/chat/send")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseBody
    public ResponseEntity<ChatMessage> sendMessage(
            @RequestParam Long conversationId,
            @RequestParam String message,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) Long orderId,
            HttpSession session) {
        try {
            Customer customer = (Customer) session.getAttribute("currentCustomer");
            ChatMessage chatMessage = chatService.sendMessage(
                conversationId,
                ChatMessage.SenderType.CUSTOMER,
                customer.getId(),
                customer.getFullName(),
                message,
                image,
                orderId
            );
            return ResponseEntity.ok(chatMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/customer/chat/{conversationId}/read")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable Long conversationId) {
        chatService.markAsRead(conversationId, ChatMessage.SenderType.CUSTOMER);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/support/chat")
    @PreAuthorize("hasRole('SUPPORT')")
    public String supportChat(Model model, HttpSession session) {
        List<ChatConversation> conversations = chatService.getActiveConversations();
        model.addAttribute("conversations", conversations);
        return "support/chat";
    }

    @GetMapping("/support/chat/{conversationId}")
    @PreAuthorize("hasRole('SUPPORT')")
    public String supportChatDetail(@PathVariable Long conversationId, Model model, HttpSession session) {
        List<ChatMessage> messages = chatService.getMessages(conversationId);
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("messages", messages);
        return "support/chat-detail";
    }

    @PostMapping("/support/chat/send")
    @PreAuthorize("hasRole('SUPPORT')")
    @ResponseBody
    public ResponseEntity<ChatMessage> supportSendMessage(
            @RequestParam Long conversationId,
            @RequestParam String message,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) Long orderId,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("currentUser");
            ChatMessage chatMessage = chatService.sendMessage(
                conversationId,
                ChatMessage.SenderType.SUPPORT,
                user.getId(),
                user.getFullName(),
                message,
                image,
                orderId
            );
            return ResponseEntity.ok(chatMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/support/chat/{conversationId}/read")
    @PreAuthorize("hasRole('SUPPORT')")
    @ResponseBody
    public ResponseEntity<?> supportMarkAsRead(@PathVariable Long conversationId) {
        chatService.markAsRead(conversationId, ChatMessage.SenderType.SUPPORT);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/support/chat/{conversationId}/close")
    @PreAuthorize("hasRole('SUPPORT')")
    @ResponseBody
    public ResponseEntity<?> closeConversation(@PathVariable Long conversationId) {
        chatService.closeConversation(conversationId);
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/chat/{conversationId}")
    @SendTo("/topic/chat/{conversationId}")
    public ChatMessage handleChatMessage(@DestinationVariable Long conversationId, Map<String, Object> messageData) {
        return null;
    }
}