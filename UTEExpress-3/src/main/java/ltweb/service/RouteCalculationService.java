package ltweb.service;

import ltweb.entity.*;
import ltweb.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteCalculationService {

	private final RouteRepository routeRepository;

	public List<RouteSegment> calculateRoute(Warehouse fromWarehouse, Warehouse toWarehouse) {
		List<RouteSegment> segments = new ArrayList<>();

		if (fromWarehouse.getId().equals(toWarehouse.getId())) {
			return segments;
		}

		Route directRoute = routeRepository
				.findByFromWarehouseIdAndToWarehouseId(fromWarehouse.getId(), toWarehouse.getId()).orElse(null);

		if (directRoute != null) {
			segments.add(new RouteSegment(fromWarehouse, toWarehouse, directRoute.getPreferredShipper(),
					directRoute.getDistanceKm(), directRoute.getEstimatedHours(), true));
		} else {
			List<Warehouse> intermediateWarehouses = findIntermediateWarehouses(fromWarehouse, toWarehouse);

			Warehouse current = fromWarehouse;
			for (int i = 0; i <= intermediateWarehouses.size(); i++) {
				Warehouse next = (i < intermediateWarehouses.size()) ? intermediateWarehouses.get(i) : toWarehouse;

				Route route = routeRepository.findByFromWarehouseIdAndToWarehouseId(current.getId(), next.getId())
						.orElse(null);

				if (route != null) {
					segments.add(new RouteSegment(current, next, route.getPreferredShipper(), route.getDistanceKm(),
							route.getEstimatedHours(), i == intermediateWarehouses.size()));
					current = next;
				}
			}
		}

		return segments;
	}

	private List<Warehouse> findIntermediateWarehouses(Warehouse from, Warehouse to) {
		List<Warehouse> path = new ArrayList<>();

		String fromCode = from.getCode();
		String toCode = to.getCode();

		if ((fromCode.equals("WH-HN") && toCode.equals("WH-HCM"))
				|| (fromCode.equals("WH-HCM") && toCode.equals("WH-HN"))) {
			return path;
		}

		return path;
	}

	@lombok.Data
	@lombok.AllArgsConstructor
	public static class RouteSegment {
		private Warehouse fromWarehouse;
		private Warehouse toWarehouse;
		private Shipper preferredShipper;
		private Double distanceKm;
		private Double estimatedHours;
		private Boolean isFinalLeg;
	}
}