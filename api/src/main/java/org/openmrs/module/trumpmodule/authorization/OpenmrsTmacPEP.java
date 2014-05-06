package org.openmrs.module.trumpmodule.authorization;

import java.util.ArrayList;
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
import luca.tmac.basic.obligations.ObligationImpl;
import luca.tmac.basic.obligations.ObligationMonitorable;

public class OpenmrsTmacPEP extends TmacPEP {
	
	private OpenmrsUserObligationMonitor openmrsOblMonitor;
	private User currentUser = Context.getAuthenticatedUser(); //currentUser is the trigger user
	private HashMap<String,String> messages;
	
	OpenmrsEnforceServiceContext SerContext = OpenmrsEnforceServiceContext.getInstance();
	private HashMap<String,Obligation> activeObs = SerContext.getActiveObs();
	private HashMap<String,List<Obligation>> userObs = SerContext.getUserObs();
	private HashMap<String,List<Obligation>> roleObs = SerContext.getRoleObs();
	private HashMap<String,List<Obligation>> oblSets = SerContext.getObligationSets();
	private List<Obligation> userObsList = new ArrayList<Obligation>();
	private List<Obligation> roleObsList = new ArrayList<Obligation>();
	
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
		String budgetfromDb = openmrsOblMonitor.getBudgetfromDB(currentUser.getId().toString());
		double previousBudget = Double.parseDouble(budgetfromDb);
		if (parser == null)
			throw new IllegalArgumentException("invalid parser id");
		
		if(methodName.equalsIgnoreCase("getPatientByUuid")){
			List<Obligation> obls = parser.getObligation();
			
			String setId = null;
			
			//firstly perform decrease budget system obligation. 
			for (Obligation obl : obls) {
				if (obl.getActionName().equalsIgnoreCase(ObligationIds.DECREASE_BUDGET_ID)) {
					String performingResult = performObligation(obl);
					messages.put(obl.getActionName(), performingResult);
					openmrsOblMonitor.updateBudgetToDB(performingResult,currentUser.getId().toString());
					setId = String.valueOf(UUID.randomUUID().getMostSignificantBits());
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
					String decreasedBudget = null;
					
					if(messages.containsKey(ObligationIds.DECREASE_BUDGET_ID)){
						double curB = Double.parseDouble(messages.get((ObligationIds.DECREASE_BUDGET_ID)));
						decreasedBudget = String.valueOf(previousBudget - curB);
						
					}
					
					Obligation obligation = null ;
					
					//get the attributeMap of the obl from policy
					HashMap<String,String> attributeMap = obl.getAttributeMap();
					
					if(obl.getActionName().contains(ObligationIds.REST_OBLIGATION_NAME_XML)){
						
						obligation = new RESTObligation((ObligationImpl)obl);
						obligation.setTriggeringUserId(currentUser.getId().toString());
						
						setOblSetId(obligation,setId);
						
						if(attributeMap.containsKey("requiredUserId")){
							String userId = attributeMap.get("requiredUserId");
							obligation.setUserId(userId);
							userObsList.add(obligation);
							userObs.put(userId, userObsList);
							
						}else if(attributeMap.containsKey("roleName")){
							//if it's a obligation to role, then don't need to set userId, just leave it null. 
							roleObsList.add(obligation);
							roleObs.put(attributeMap.get("roleName"), roleObsList);
						}else 
							obligation.setUserId(currentUser.getId().toString());
					}else{
						
						obligation = new EmailObligation(obl.getActionName(),currentUser.getId().toString(),obl.getStartDate(),null);
						obligation.setAttributeMap(attributeMap);
						obligation.setUserId(currentUser.getId().toString());//userid and triggeringUiserId are the same.
						setOblSetId(obligation,setId);
						
					}
					
					
					
					obligation.setDecreasedBudget(decreasedBudget);	
					
					activeObs.put(obligation.getObUUID(), obligation);
					SerContext.setActiveObs(activeObs);
					SerContext.setUserObs(userObs);
					SerContext.setRoleObs(roleObs);
					
					String message = obl.getAttribute("message") + "your UUID of the obligation is : "+ obligation.getObUUID().toString();
					messages.put(obl.getActionName(), message);
				}
			}
			System.err.println("the size of the activeObs is : " + activeObs.size());
			System.err.println("the size of the userObs is : " + userObs.size());
			System.err.println("the size of the roleObs is : " + roleObs.size());
		}
		openmrsOblMonitor.checkObligations();
		return messages;
	}

  
  public void updateBudget(String currentBudget){
	
	List<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
	attributes.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, currentBudget,
			StringAttribute.identifier));
	dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, currentUser.getId().toString(), attributes);
	
  }
  
  public void setOblSetId(Obligation obligation, String setId){
	  if(obligation.getAttribute("setName").contains(ObligationIds.BUDGET_DECREASE_SET_XML)){
			obligation.setSetId(setId);
			if(oblSets.containsKey(setId)){
				oblSets.get(setId).add(obligation); 
				System.err.println("Anita, the oblSets has contains this setId : " +setId+", and the "
						+ "there are " + oblSets.get(setId).size() + "obligations with the same setId.");
			}else{
				List<Obligation> obls = new ArrayList<Obligation>();
				obls.add(obligation);
				oblSets.put(setId,obls);
			}
		}else {
			String newSetId = String.valueOf(UUID.randomUUID().getMostSignificantBits());
			obligation.setSetId(newSetId);
			List<Obligation> obls = new ArrayList<Obligation>();
			obls.add(obligation);
			oblSets.put(newSetId,obls);
		}
	  System.err.println("Anita, the size of the oblSets is : " + oblSets.size());
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
