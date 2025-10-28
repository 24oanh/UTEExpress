package ltweb.service;

import ltweb.entity.*;
import ltweb.repository.ShipmentLegRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentLegService {

	private final ShipmentLegRepository shipmentLegRepository;
	private final RouteCalculationService routeCalculationService;

	@Transactional
	public void createShipmentLegs(Shipment shipment, Order order) {
		List<RouteCalculationService.RouteSegment> route = routeCalculationService.calculateRoute(order.getWarehouse(),
				order.getDestinationWarehouse());

		for (int i = 0; i < route.size(); i++) {
			RouteCalculationService.RouteSegment segment = route.get(i);

			ShipmentLeg leg = ShipmentLeg.builder().shipment(shipment).order(order)
					.fromWarehouse(segment.getFromWarehouse()).toWarehouse(segment.getToWarehouse())
					.shipper(segment.getPreferredShipper()).legOrder(i + 1).legSequence(i + 1)
					.distanceKm(segment.getDistanceKm()).estimatedHours(segment.getEstimatedHours())
					.isFinalLeg(segment.getIsFinalLeg())
					.status(i == 0 ? ShipmentStatus.PENDING : ShipmentStatus.PENDING).build();

			shipmentLegRepository.save(leg);
		}
	}

	public RouteCalculationService.RouteSegment getFirstLegPreferredShipper(Order order) {
		List<RouteCalculationService.RouteSegment> route = routeCalculationService.calculateRoute(order.getWarehouse(),
				order.getDestinationWarehouse());
		return route.isEmpty() ? null : route.get(0);
	}
}