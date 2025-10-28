package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_legs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentLeg {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "shipment_id", nullable = false)
	private Shipment shipment;

	@ManyToOne
	@JoinColumn(name = "from_warehouse_id", nullable = false)
	private Warehouse fromWarehouse;

	@ManyToOne
	@JoinColumn(name = "to_warehouse_id")
	private Warehouse toWarehouse;

	@ManyToOne
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;
	
	@ManyToOne
	@JoinColumn(name = "shipper_id")
	private Shipper shipper;

	@Column(name = "leg_order", nullable = false)
	private Integer legOrder;

	@Column(name = "distance_km")
	private Double distanceKm;

	@Column(name = "estimated_hours")
	private Double estimatedHours;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ShipmentStatus status;

	@Column(name = "pickup_time")
	private LocalDateTime pickupTime;

	@Column(name = "delivery_time")
	private LocalDateTime deliveryTime;

	@Column(columnDefinition = "NVARCHAR(MAX)")
	private String notes;

	@Column(name = "is_final_leg")
	private Boolean isFinalLeg;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "leg_sequence", nullable = false)
	private Integer legSequence;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		status = ShipmentStatus.PENDING;
		isFinalLeg = false;
		if (legSequence == null) {
			legSequence = 1;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}