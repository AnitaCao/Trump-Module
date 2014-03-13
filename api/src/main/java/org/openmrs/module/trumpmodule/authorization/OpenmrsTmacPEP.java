package org.openmrs.module.trumpmodule.authorization;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.obligations.OpenmrsUserObligationMonitor;
import org.openmrs.module.trumpmodule.obligations.UserObRelation;
import org.wso2.balana.attr.StringAttribute;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.tmac.basic.ResponseParser;
import luca.tmac.basic.TmacPEP;
import luca.tmac.basic.data.xml.SubjectAttributeXmlName;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationIds;
import luca.tmac.basic.obligations.ObligationMonitorable;

public class OpenmrsTmacPEP extends TmacPEP{
	
	public OpenmrsUserObligationMonitor openmrsOblMonitor;
	public User user = Context.getAuthenticatedUser();
	public HashMap<String,String> messages;
	public OpenmrsTmacPEP(DataHandler parDataHandler,
			ObligationMonitorable monitorable) {
		super(parDataHandler, monitorable);
		
		openmrsOblMonitor = new OpenmrsUserObligationMonitor(new ArrayList<Obligation>(),this,dh);
		
	}
	
	public HashMap<String,String> acceptResponse(long parserId,String methodName) {
		ResponseParser parser = sessionParsers.get(parserId);
		messages = new HashMap<String,String>();
		double previousBudget = Double.parseDouble(getBudgetfromDb());
		//ObligationSet oblSet = new ObligationSet(new ArrayList<Obligation>(),dh);
		if (parser == null)
			throw new IllegalArgumentException("invalid parser id");
		
		if(methodName.equalsIgnoreCase("getPatients")){
			List<Obligation> obls = parser.getObligation().getList();
			
			//firstly perform decrease budget system obligation. 
			for (Obligation obl : obls) {
				if (obl.actionName.equalsIgnoreCase(ObligationIds.DECREASE_BUDGET_ID)) {
					String performingResult = performObligation(obl);
					messages.put(obl.actionName, performingResult);
					//decreaseBudgettoDB(); //update database if there is a system obligation named decrease budget
					updateBudget(performingResult);
					obls.remove(obl);
				}
			}
			
			for (Obligation obl : obls) {
			
				if (obl.isSystemObligation()) {
					
					String performingResult = performObligation(obl);
					messages.put(obl.actionName, performingResult);
					//decreaseBudgettoDB(); //update database if there is a system obligation named decrease budget
					
				} else {
					//System.out.println(obl.actionName);
					
					//if it's not system obligation which means it's a user obligation, then we should add the obligation to the openmrs context
					Date startTime = new Date();
					String decreasedBudget = null;
					
					if(messages.containsKey(ObligationIds.DECREASE_BUDGET_ID)){
						double curB = Double.parseDouble(messages.get((ObligationIds.DECREASE_BUDGET_ID)));
						decreasedBudget = String.valueOf(previousBudget - curB);
					}
					
					UserObRelation uo = new UserObRelation(user.getId().toString(),obl.getAttribute("id"),startTime,decreasedBudget);
					UUID uuid = UUID.randomUUID();
					OpenmrsEnforceServiceContext.getActiveOb().put(uuid,uo);
					String message = obl.getAttribute("message") + "your UUID of the obligation is : "+ uuid.toString();
					messages.put(obl.actionName, message);
				}
			}
			
		}
		openmrsOblMonitor.checkObligations();
		return messages;
	}
	
//	public void decreaseBudgettoDB(){
//		//if(messages.containsKey(ObligationIds.DECREASE_BUDGET_ID)){
//			String currentBudget = messages.get(ObligationIds.DECREASE_BUDGET_ID); //this message will contain the current budget and the decreased budget. 
//			updateBudget(currentBudget);
//    	
//			//System.err.println("Anita , currentBudget is : " + currentBudget + " , the privous budget is : " + previousBudget);
//		//}
//	}
	
  public void updateBudget(String currentBudget){
	
	List<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
	attributes.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, currentBudget,
			StringAttribute.identifier));
	dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, user.getId().toString(), attributes);
	
}
  public String getBudgetfromDb(){
	ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
	query.add(new AttributeQuery(SubjectAttributeXmlName.ID, user.getId().toString(),StringAttribute.identifier));
	List<String> budgets = dh.getAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query, "budget");
	return budgets.get(0);
}


	public String performObligation(Obligation obl)
	{
		if(obl.actionName.equals(ObligationIds.SHOW_DENY_REASON_OBLIGATION_ID))
		{
			String message = obl.getAttribute("message");
			return message;
			
		}else if(obl.actionName.equals(ObligationIds.DECREASE_BUDGET_ID)){
			
			double neededBudget = Double.parseDouble(obl.getAttribute("needed-budget"));
			double budget = Double.parseDouble(obl.getAttribute("budget"));
			String currentBudgetString = String.valueOf(budget-neededBudget);
			return currentBudgetString;															
			
		}
		else throw new IllegalArgumentException("system obligation not supported:" + obl.actionName);
	}
}
