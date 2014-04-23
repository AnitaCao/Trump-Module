package org.openmrs.module.trumpmodule.authorization;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.obligations.EmailObligation;
import org.openmrs.module.trumpmodule.obligations.OpenmrsUserObligationMonitor;
import org.openmrs.module.trumpmodule.obligations.RESTObligation;
import org.wso2.balana.attr.StringAttribute;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.tmac.basic.ResponseParser;
import luca.tmac.basic.TmacPEP;
import luca.tmac.basic.data.xml.SubjectAttributeXmlName;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationIds;
import luca.tmac.basic.obligations.ObligationMonitorable;

public class OpenmrsTmacPEP extends TmacPEP {
	
	private OpenmrsUserObligationMonitor openmrsOblMonitor;
	private User user = Context.getAuthenticatedUser();
	private HashMap<String,String> messages;
	
	OpenmrsEnforceServiceContext SerContext = OpenmrsEnforceServiceContext.getInstance();
	private ArrayList<Obligation> activeObs = SerContext.getActiveObs();
	
	public OpenmrsTmacPEP(DataHandler parDataHandler,
			ObligationMonitorable monitorable) {
		super(parDataHandler, monitorable);
		
		openmrsOblMonitor = new OpenmrsUserObligationMonitor(new ArrayList<Obligation>(),monitorable,dh);
		
	}
	
	@Override
	public String getUserPolicyDirectory() {
		return OpenmrsEnforceServiceContext.getInstance().getUserPolicyDirectory();
	}


	@Override
	public String getTopLevelPolicyDirectory() {
		return OpenmrsEnforceServiceContext.getInstance().getTopLevelPolicyDirectory();
	}


	public HashMap<String,String> acceptResponse(long parserId,String methodName) {
		ResponseParser parser = sessionParsers.get(parserId);
		messages = new HashMap<String,String>();
		String budgetfromDb = openmrsOblMonitor.getBudgetfromDb(user.getId().toString());
		double previousBudget = Double.parseDouble(budgetfromDb);
		if (parser == null)
			throw new IllegalArgumentException("invalid parser id");
		
		if(methodName.equalsIgnoreCase("getPatientByUuid")){
			List<Obligation> obls = parser.getObligation().getList();
			
			//firstly perform decrease budget system obligation. 
			for (Obligation obl : obls) {
				if (obl.getActionName().equalsIgnoreCase(ObligationIds.DECREASE_BUDGET_ID)) {
					String performingResult = performObligation(obl);
					messages.put(obl.getActionName(), performingResult);
					openmrsOblMonitor.updateBudget(performingResult,user.getId().toString());
				}
			}
			
			for (Obligation obl : obls) {
			
				if (obl.isSystemObligation()) {
					if(!obl.getActionName().equalsIgnoreCase(ObligationIds.DECREASE_BUDGET_ID)){
						String performingResult = performObligation(obl);
						messages.put(obl.getActionName(), performingResult);
					}
				} else {
					//if it's not system obligation which means it's a user obligation, then we should add the obligation to the openmrs context
					Date startTime = new Date();
					String decreasedBudget = null;
					
					if(messages.containsKey(ObligationIds.DECREASE_BUDGET_ID)){
						double curB = Double.parseDouble(messages.get((ObligationIds.DECREASE_BUDGET_ID)));
						decreasedBudget = String.valueOf(previousBudget - curB);
					}
					
					//UserObRelation uo = new UserObRelation(user.getId().toString(),obl.getAttribute("id"),startTime,decreasedBudget);
					
					Obligation ob = null ;
					
					ArrayList<AttributeQuery> newAttList = new ArrayList<AttributeQuery>();
					
					if(obl.getActionName().equals(ObligationIds.REST_OBLIGATION_NAME_XML)){
						
						ob = new RESTObligation(obl.getActionName(),user.getId().toString(),startTime,newAttList);
					}else{
						//ob = new ObligationImpl(obl.getActionName(),user.getId().toString(),startTime,newAttList);
						ob = new EmailObligation(obl.getActionName(),user.getId().toString(),startTime,newAttList);
					}
					ob.setDecreasedBudget(decreasedBudget);
					ob.setObUUID(UUID.randomUUID());
					
					
					activeObs.add(ob);
					SerContext.setActiveObs(activeObs);
					
					String message = obl.getAttribute("message") + "your UUID of the obligation is : "+ ob.getObUUID().toString();
					
					System.err.println("the size of the obligation is : " + activeObs.size());
					
					messages.put(obl.getActionName(), message);
				}
			}
		}
		openmrsOblMonitor.checkObligations();
		return messages;
	}

  
  public void updateBudget(String currentBudget){
	
	List<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
	attributes.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, currentBudget,
			StringAttribute.identifier));
	dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, user.getId().toString(), attributes);
	
}

	public String performObligation(Obligation obl)
	{
		if(obl.getActionName().equals(ObligationIds.SHOW_DENY_REASON_OBLIGATION_ID))
		{
			String message = obl.getAttribute("message");
			return message;
			
		}else if(obl.getActionName().equals(ObligationIds.SHOW_PERMIT_MESSAGE_OBLIGATION_ID)){
			String message = obl.getAttribute("message");
			return message;
			
		}else if(obl.getActionName().equals(ObligationIds.DECREASE_BUDGET_ID)){
			
			double neededBudget = Double.parseDouble(obl.getAttribute("needed-budget"));
			double budget = Double.parseDouble(obl.getAttribute("budget"));
			String currentBudgetString = String.valueOf(budget-neededBudget);
			return currentBudgetString;															
			
		}
		else throw new IllegalArgumentException("system obligation not supported:" + obl.getActionName());
	}
}
