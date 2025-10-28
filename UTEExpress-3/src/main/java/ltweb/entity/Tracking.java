package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trackings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TrackingStatus status; // STARTED, IN_PROGRESS, COMPLETED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}