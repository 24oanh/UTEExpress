package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "support_user_id")
    private User supportUser;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConversationStatus status;

    @Column(name = "unread_customer_count")
    private Integer unreadCustomerCount;

    @Column(name = "unread_support_count")
    private Integer unreadSupportCount;

    @Column(name = "last_message", columnDefinition = "NVARCHAR(MAX)")
    private String lastMessage;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = ConversationStatus.ACTIVE;
        unreadCustomerCount = 0;
        unreadSupportCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ConversationStatus {
        ACTIVE, CLOSED
    }
}