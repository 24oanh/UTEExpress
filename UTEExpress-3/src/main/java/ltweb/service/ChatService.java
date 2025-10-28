package ltweb.service;

import ltweb.entity.*;
import ltweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatConversation getOrCreateConversation(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        return conversationRepository.findByCustomerIdAndStatus(customerId, 
                ChatConversation.ConversationStatus.ACTIVE)
                .orElseGet(() -> {
                    ChatConversation conversation = ChatConversation.builder()
                            .customer(customer)
                            .status(ChatConversation.ConversationStatus.ACTIVE)
                            .build();
                    return conversationRepository.save(conversation);
                });
    }

    @Transactional
    public ChatMessage sendMessage(Long conversationId, ChatMessage.SenderType senderType, 
                                   Long senderId, String senderName, String message, 
                                   MultipartFile image, Long orderId) throws IOException {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = cloudinaryService.uploadFile(image);
        }

        Order order = null;
        if (orderId != null) {
            order = orderRepository.findById(orderId).orElse(null);
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .conversation(conversation)
                .senderType(senderType)
                .senderId(senderId)
                .senderName(senderName)
                .message(message)
                .imageUrl(imageUrl)
                .order(order)
                .build();

        chatMessage = messageRepository.save(chatMessage);

        conversation.setLastMessage(message);
        conversation.setLastMessageTime(chatMessage.getCreatedAt());
        
        if (senderType == ChatMessage.SenderType.CUSTOMER) {
            conversation.setUnreadSupportCount(conversation.getUnreadSupportCount() + 1);
        } else {
            conversation.setUnreadCustomerCount(conversation.getUnreadCustomerCount() + 1);
        }
        
        conversationRepository.save(conversation);

        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, chatMessage);

        if (senderType == ChatMessage.SenderType.CUSTOMER) {
            messagingTemplate.convertAndSend("/topic/support/new-message", conversation);
        }

        return chatMessage;
    }

    public List<ChatMessage> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public void markAsRead(Long conversationId, ChatMessage.SenderType readerType) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (readerType == ChatMessage.SenderType.CUSTOMER) {
            conversation.setUnreadCustomerCount(0);
        } else {
            conversation.setUnreadSupportCount(0);
        }
        
        conversationRepository.save(conversation);
    }

    public List<ChatConversation> getActiveConversations() {
        return conversationRepository.findByStatusOrderByLastMessageTimeDesc(
                ChatConversation.ConversationStatus.ACTIVE);
    }

    public List<ChatConversation> getSupportConversations(Long supportUserId) {
        return conversationRepository.findBySupportUserIdOrderByLastMessageTimeDesc(supportUserId);
    }

    @Transactional
    public void assignToSupport(Long conversationId, Long supportUserId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        User supportUser = userRepository.findById(supportUserId)
                .orElseThrow(() -> new RuntimeException("Support user not found"));
        
        conversation.setSupportUser(supportUser);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void closeConversation(Long conversationId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        conversation.setStatus(ChatConversation.ConversationStatus.CLOSED);
        conversationRepository.save(conversation);
    }

    public ChatConversation getConversationById(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }

    public long getTotalUnreadForAdmin() {
        List<ChatConversation> conversations = getActiveConversations();
        return conversations.stream()
                .mapToLong(ChatConversation::getUnreadSupportCount)
                .sum();
    }

    public List<ChatConversation> getConversationsForAdmin() {
        return conversationRepository.findByStatusOrderByLastMessageTimeDesc(
                ChatConversation.ConversationStatus.ACTIVE);
    }
}