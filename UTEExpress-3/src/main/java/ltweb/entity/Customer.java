package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String customerCode;
    
    @Column(nullable = false)
    private String fullName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false, unique = true)
    private String phone;
    
    @Column(nullable = false)
    private String address;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "is_email_verified")
    private Boolean isEmailVerified;
    
    @Column(name = "is_phone_verified")
    private Boolean isPhoneVerified;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;
    
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        customerCode = "CUST" + System.currentTimeMillis();
        isEmailVerified = false;
        isPhoneVerified = false;
        status = CustomerStatus.PENDING_VERIFICATION;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}