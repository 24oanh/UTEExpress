package ltweb.service;

import ltweb.entity.NotificationType;
import ltweb.entity.Order;
import ltweb.entity.Shipment;
import ltweb.entity.Tracking;
import ltweb.entity.TrackingStatus;
import ltweb.repository.TrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingService {

	private final TrackingRepository trackingRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final NotificationService notificationService;
	public List<Tracking> getAllTrackings() {
		return trackingRepository.findAll();
	}

	public Tracking getTrackingById(Long id) {
		return trackingRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Tracking not found with id: " + id));
	}

	public List<Tracking> getTrackingsByShipmentId(Long shipmentId) {
		return trackingRepository.findByShipmentIdOrderByCreatedAtDesc(shipmentId);
	}

	@Transactional
	public Tracking createTracking(Shipment shipment, Double latitude, Double longitude, String description,
	        TrackingStatus status) {
	    Tracking tracking = Tracking.builder()
	            .shipment(shipment)
	            .latitude(latitude)
	            .longitude(longitude)
	            .description(description)
	            .status(status)
	            .build();
	    
	    Tracking savedTracking = trackingRepository.save(tracking);
	    sendRealtimeTracking(shipment.getId(), savedTracking);
	    
	    // Thông báo cho customer khi có tracking mới
	    Order order = shipment.getOrder();
	    if (order != null && order.getCustomer() != null) {
	        notificationService.createCustomerNotification(
	            order.getCustomer().getId(),
	            description,
	            NotificationType.ORDER_ASSIGNED,
	            order
	        );
	    }
	    
	    return savedTracking;
	}

	@Transactional
	public Tracking updateShipmentLocation(Long shipmentId, Double latitude, Double longitude, String description) {
		Tracking tracking = Tracking.builder().shipment(Shipment.builder().id(shipmentId).build()).latitude(latitude)
				.longitude(longitude).description(description != null ? description : "Location updated")
				.status(TrackingStatus.IN_PROGRESS).build();

		Tracking savedTracking = trackingRepository.save(tracking);

		sendRealtimeTracking(shipmentId, savedTracking);

		return savedTracking;
	}

	private void sendRealtimeTracking(Long shipmentId, Tracking tracking) {
		String destination = "/topic/tracking/" + shipmentId;
		messagingTemplate.convertAndSend(destination, tracking);
	}

	public void broadcastLocationUpdate(Long shipmentId, Double latitude, Double longitude) {
		String destination = "/topic/tracking/" + shipmentId;
		TrackingUpdate update = new TrackingUpdate(shipmentId, latitude, longitude);
		messagingTemplate.convertAndSend(destination, update);
	}

	private static class TrackingUpdate {
		public Long shipmentId;
		public Double latitude;
		public Double longitude;

		public TrackingUpdate(Long shipmentId, Double latitude, Double longitude) {
			this.shipmentId = shipmentId;
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}
}