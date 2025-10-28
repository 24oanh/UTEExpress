package ltweb.controller;

import ltweb.entity.Order;
import ltweb.entity.Shipment;
import ltweb.entity.Tracking;
import ltweb.service.OrderService;
import ltweb.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final OrderService orderService;


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
    
    @GetMapping("/api/tracking/order/{orderCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderTracking(@PathVariable String orderCode) {
        try {
            Order order = orderService.getOrderByCode(orderCode);
            Shipment shipment = orderService.getShipmentByOrderId(order.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderCode", order.getOrderCode());
            response.put("status", order.getStatus().name());
            response.put("senderName", order.getSenderName());
            response.put("recipientName", order.getRecipientName());
            response.put("serviceType", order.getServiceType().name());
            response.put("warehouse", order.getWarehouse().getCode());
            response.put("destinationWarehouse", order.getDestinationWarehouse().getCode());
            
            if (shipment != null) {
                List<Tracking> trackings = trackingService.getTrackingsByShipmentId(shipment.getId());
                List<Map<String, Object>> trackingList = trackings.stream().map(t -> {
                    Map<String, Object> trackMap = new HashMap<>();
                    trackMap.put("description", t.getDescription());
                    trackMap.put("createdAt", t.getCreatedAt());
                    return trackMap;
                }).collect(Collectors.toList());
                response.put("trackings", trackingList);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}