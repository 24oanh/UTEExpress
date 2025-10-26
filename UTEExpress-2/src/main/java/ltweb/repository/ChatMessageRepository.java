package ltweb.repository;

import ltweb.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    long countByConversationIdAndSenderTypeAndIsRead(Long conversationId, ChatMessage.SenderType senderType, Boolean isRead);
}

