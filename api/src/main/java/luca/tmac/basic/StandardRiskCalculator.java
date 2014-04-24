package luca.tmac.basic;

public class StandardRiskCalculator implements RiskCalculator {

	@Override
	public double calculateRisk(Double teamTrustworthiness, String taskId) {
		return 1 - teamTrustworthiness;
	}

}
