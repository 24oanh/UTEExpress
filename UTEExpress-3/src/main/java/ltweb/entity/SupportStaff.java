package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportStaff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(name = "is_online")
    private Boolean isOnline;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isOnline = false;
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}