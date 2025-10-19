package ltweb.controller;

import ltweb.entity.Tracking;
import ltweb.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping("/api/tracking/shipment/{shipmentId}")
    @ResponseBody
    public ResponseEntity<List<Tracking>> getTrackingByShipment(@PathVariable Long shipmentId) {
        try {
            List<Tracking> trackings = trackingService.getTrackingsByShipmentId(shipmentId);
            return ResponseEntity.ok(trackings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/tracking/update")
    @ResponseBody
    public ResponseEntity<Tracking> updateTracking(@RequestParam Long shipmentId,
                                                   @RequestParam Double latitude,
                                                   @RequestParam Double longitude,
                                                   @RequestParam(required = false) String description) {
        try {
            Tracking tracking = trackingService.updateShipmentLocation(shipmentId, latitude, longitude, description);
            return ResponseEntity.ok(tracking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @MessageMapping("/tracking/{shipmentId}")
    @SendTo("/topic/tracking/{shipmentId}")
    public Map<String, Object> updateLocation(@DestinationVariable Long shipmentId,
                                             Map<String, Object> locationData) {
        try {
            Double latitude = ((Number) locationData.get("latitude")).doubleValue();
            Double longitude = ((Number) locationData.get("longitude")).doubleValue();
            String description = (String) locationData.get("description");
            
            trackingService.updateShipmentLocation(shipmentId, latitude, longitude, description);
            trackingService.broadcastLocationUpdate(shipmentId, latitude, longitude);
            
            return Map.of(
                "success", true,
                "shipmentId", shipmentId,
                "latitude", latitude,
                "longitude", longitude
            );
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @MessageMapping("/location/update")
    @SendTo("/topic/location")
    public Map<String, Object> broadcastLocation(Map<String, Object> locationData) {
        return locationData;
    }
}