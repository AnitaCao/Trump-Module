package luca.tmac.basic.data.uris;

public class RiskAttributeURI {
	public static final String RISK_CATEGORY_URI = "luca:tmac:risk-category:risk";
	public static final String RISK_VALUE_URI = RISK_CATEGORY_URI + ":" + "risk-value";
	public static final String TRUSTWORTHINESS_URI = RISK_CATEGORY_URI + ":" + "trustworthiness";
	public static final String BUDGET_URI = RISK_CATEGORY_URI + ":" + "needed-budget";
}
