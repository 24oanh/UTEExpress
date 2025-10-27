package ltweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "from_warehouse_id", nullable = false)
	private Warehouse fromWarehouse;

	@ManyToOne
	@JoinColumn(name = "to_warehouse_id", nullable = false)
	private Warehouse toWarehouse;

	@ManyToOne
	@JoinColumn(name = "preferred_shipper_id")
	private Shipper preferredShipper;

	@Column(name = "distance_km")
	private Double distanceKm;

	@Column(name = "estimated_hours")
	private Double estimatedHours;

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		isActive = true;
	}
}