package ltweb.service;

import ltweb.entity.ServiceType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ShippingFeeService {

	private static final BigDecimal BASE_FEE = new BigDecimal("20000");
	private static final BigDecimal PRICE_PER_KM = new BigDecimal("3000");
	private static final BigDecimal PRICE_PER_KG = new BigDecimal("5000");

	private static final BigDecimal STANDARD_MULTIPLIER = new BigDecimal("1.0");
	private static final BigDecimal EXPRESS_MULTIPLIER = new BigDecimal("1.5");
	private static final BigDecimal ECONOMY_MULTIPLIER = new BigDecimal("0.8");

	public BigDecimal calculateShippingFee(Double distance, Double weight, ServiceType serviceType) {
		BigDecimal distanceFee = PRICE_PER_KM.multiply(new BigDecimal(distance));
		BigDecimal weightFee = PRICE_PER_KG.multiply(new BigDecimal(weight));

		BigDecimal totalFee = BASE_FEE.add(distanceFee).add(weightFee);

		BigDecimal multiplier = getServiceTypeMultiplier(serviceType);
		totalFee = totalFee.multiply(multiplier);

		return totalFee.setScale(0, RoundingMode.HALF_UP);
	}

	private BigDecimal getServiceTypeMultiplier(ServiceType serviceType) {
		switch (serviceType) {
		case EXPRESS:
			return EXPRESS_MULTIPLIER;
		case ECONOMY:
			return ECONOMY_MULTIPLIER;
		case STANDARD:
		default:
			return STANDARD_MULTIPLIER;
		}
	}

	public int getEstimatedDeliveryDays(Double distance, ServiceType serviceType) {
		int baseDays = (int) Math.ceil(distance / 500);

		switch (serviceType) {
		case EXPRESS:
			return Math.max(1, baseDays - 1);
		case ECONOMY:
			return baseDays + 2;
		case STANDARD:
		default:
			return baseDays;
		}
	}
}