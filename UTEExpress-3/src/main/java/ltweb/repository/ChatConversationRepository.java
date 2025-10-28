package ltweb.repository;

import ltweb.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findByCustomerIdAndStatus(Long customerId, ChatConversation.ConversationStatus status);
    List<ChatConversation> findByStatusOrderByLastMessageTimeDesc(ChatConversation.ConversationStatus status);
    List<ChatConversation> findBySupportUserIdOrderByLastMessageTimeDesc(Long supportUserId);
    List<ChatConversation> findByCustomerIdOrderByLastMessageTimeDesc(Long customerId);
}

