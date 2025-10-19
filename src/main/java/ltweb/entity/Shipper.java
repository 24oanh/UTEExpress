package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "shippers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "total_deliveries")
    private Integer totalDeliveries;

    @Column(name = "successful_deliveries")
    private Integer successfulDeliveries;

    @Column(name = "failed_deliveries")
    private Integer failedDeliveries;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"warehouse", "shipper"}) 
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        totalDeliveries = 0;
        successfulDeliveries = 0;
        failedDeliveries = 0;
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}