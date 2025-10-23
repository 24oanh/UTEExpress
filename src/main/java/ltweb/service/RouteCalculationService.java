package ltweb.service;

import ltweb.entity.*;
import ltweb.repository.RouteRepository;
import ltweb.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteCalculationService {

	private final RouteRepository routeRepository;
	private final WarehouseRepository warehouseRepository;

	// RouteCalculationService.java - Thay thế method calculateRoute
	public List<RouteSegment> calculateRoute(Warehouse fromWarehouse, Warehouse toWarehouse) {
		List<RouteSegment> segments = new ArrayList<>();

		if (fromWarehouse.getId().equals(toWarehouse.getId())) {
			return segments;
		}

		String fromCode = fromWarehouse.getCode();
		String toCode = toWarehouse.getCode();

		// HCM -> HN: phải qua DN
		if (fromCode.equals("WH-HCM") && toCode.equals("WH-HN")) {
			Warehouse danangWarehouse = warehouseRepository.findByCode("WH-DN")
					.orElseThrow(() -> new RuntimeException("Warehouse DN not found"));

			Route hcmToDn = routeRepository.findByFromWarehouseIdAndToWarehouseId(
					fromWarehouse.getId(), danangWarehouse.getId()).orElse(null);
			Route dnToHn = routeRepository.findByFromWarehouseIdAndToWarehouseId(
					danangWarehouse.getId(), toWarehouse.getId()).orElse(null);

			if (hcmToDn != null) {
				segments.add(new RouteSegment(fromWarehouse, danangWarehouse,
						hcmToDn.getPreferredShipper(), hcmToDn.getDistanceKm(),
						hcmToDn.getEstimatedHours(), false));
			}
			if (dnToHn != null) {
				segments.add(new RouteSegment(danangWarehouse, toWarehouse,
						dnToHn.getPreferredShipper(), dnToHn.getDistanceKm(),
						dnToHn.getEstimatedHours(), true));
			}
			return segments;
		}

		// HN -> HCM: phải qua DN
		if (fromCode.equals("WH-HN") && toCode.equals("WH-HCM")) {
			Warehouse danangWarehouse = warehouseRepository.findByCode("WH-DN")
					.orElseThrow(() -> new RuntimeException("Warehouse DN not found"));

			Route hnToDn = routeRepository.findByFromWarehouseIdAndToWarehouseId(
					fromWarehouse.getId(), danangWarehouse.getId()).orElse(null);
			Route dnToHcm = routeRepository.findByFromWarehouseIdAndToWarehouseId(
					danangWarehouse.getId(), toWarehouse.getId()).orElse(null);

			if (hnToDn != null) {
				segments.add(new RouteSegment(fromWarehouse, danangWarehouse,
						hnToDn.getPreferredShipper(), hnToDn.getDistanceKm(),
						hnToDn.getEstimatedHours(), false));
			}
			if (dnToHcm != null) {
				segments.add(new RouteSegment(danangWarehouse, toWarehouse,
						dnToHcm.getPreferredShipper(), dnToHcm.getDistanceKm(),
						dnToHcm.getEstimatedHours(), true));
			}
			return segments;
		}

		// Các route trực tiếp còn lại
		Route directRoute = routeRepository.findByFromWarehouseIdAndToWarehouseId(
				fromWarehouse.getId(), toWarehouse.getId()).orElse(null);

		if (directRoute != null) {
			segments.add(new RouteSegment(fromWarehouse, toWarehouse,
					directRoute.getPreferredShipper(), directRoute.getDistanceKm(),
					directRoute.getEstimatedHours(), true));
		}

		return segments;
	}

	// private List<Warehouse> findIntermediateWarehouses(Warehouse from, Warehouse to) {
	// 	List<Warehouse> path = new ArrayList<>();

	// 	String fromCode = from.getCode();
	// 	String toCode = to.getCode();

	// 	if ((fromCode.equals("WH-HN") && toCode.equals("WH-HCM"))
	// 			|| (fromCode.equals("WH-HCM") && toCode.equals("WH-HN"))) {
	// 		return path;
	// 	}

	// 	return path;
	// }

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