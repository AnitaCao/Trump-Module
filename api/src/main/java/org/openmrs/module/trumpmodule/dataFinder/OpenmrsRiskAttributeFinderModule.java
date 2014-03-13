package org.openmrs.module.trumpmodule.dataFinder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.DoubleAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;

import luca.data.DataHandler;
import luca.tmac.basic.NeededBudgetCalculator;
import luca.tmac.basic.RiskCalculator;
import luca.tmac.basic.TrustCalculator;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.uris.RiskAttributeURI;



public class OpenmrsRiskAttributeFinderModule extends AbstractAttributeFinderModule {


	private Set<String> categories;
	private Set<String> ids;

	private static URI team_category_uri = null;
	private static URI team_id_uri = null;
	private static URI task_category_uri = null;
	private static URI task_id_uri = null;
	
	DataHandler data = null;
	TrustCalculator trustCalc = null;
	RiskCalculator riskCalc = null;
	NeededBudgetCalculator budgetCalc = null;

	public OpenmrsRiskAttributeFinderModule(DataHandler pData, TrustCalculator tc,RiskCalculator rc, NeededBudgetCalculator bc) {

		this.trustCalc = tc;
		this.riskCalc = rc;
		this.budgetCalc = bc;

		// set Data Handler
		data = pData;

		// set supported categories
		categories = new HashSet<String>();
		categories.add(RiskAttributeURI.RISK_CATEGORY_URI);

		// set supported ids
		ids = new HashSet<String>();
		ids.add(RiskAttributeURI.TRUSTWORTHINESS_URI);
		ids.add(RiskAttributeURI.BUDGET_URI);
	}
	public OpenmrsRiskAttributeFinderModule(DataHandler pData, NeededBudgetCalculator bc){
		
		this.budgetCalc = bc;

		// set Data Handler
		data = pData;

		// set supported categories
		categories = new HashSet<String>();
		categories.add(RiskAttributeURI.RISK_CATEGORY_URI);

		// set supported ids
		ids = new HashSet<String>();
		ids.add(RiskAttributeURI.TRUSTWORTHINESS_URI);
		ids.add(RiskAttributeURI.BUDGET_URI);
	}
	@Override
	public Set<String> getSupportedCategories() {
		return categories;
	}

	@Override
	public Set<String> getSupportedIds() {
		return ids;
	}

	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId,
			String issuer, URI category, EvaluationCtx context) {

		if (!getSupportedCategories().contains(category.toString()))
			return new EvaluationResult(getEmptyBag());


		AttributeValue AttResult = findAttributes(
				attributeId);

		return new EvaluationResult(AttResult);
	}

	private AttributeValue findAttributes(
			URI attributeURI) {
		
		 if(attributeURI.toString().equals(RiskAttributeURI.BUDGET_URI))
		{
			double budget = computeBudget();
			ArrayList<DoubleAttribute> values = new ArrayList<DoubleAttribute>();
			values.add(new DoubleAttribute(budget));

			URI type = null;
			try {
				type = new URI(DoubleAttribute.identifier);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			return new BagAttribute(type, values);
		}
		
		return null;
	}

	public double computeBudget()
	{	double budget = budgetCalc.calculateBudget();
		return budget;
	}
	
	
//compute trust
//compute risk


}

