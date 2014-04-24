package luca.tmac.basic;

public interface RiskCalculator {
	
	/**
	 * method for calculate the risk of a permission of user given a
	 * the trustworthiness of a group and the permission
	 * @param teamTrustworthiness trustworthiness value of the team
	 * @param permissionId Id of the requested permission
	 * @return risk value for the request
	 * */
	double calculateRisk(Double teamTrustworthiness,String taskId );
}
