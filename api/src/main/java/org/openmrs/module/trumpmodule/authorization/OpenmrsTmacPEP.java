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
	private User currentUser = Context.getAuthenticatedUser(); //currentUser is the triggering user of the obligation
	private HashMap<String,String> messages;
	
	OpenmrsEnforceServiceContext SerContext = OpenmrsEnforceServiceContext.getInstance();
//	private List<Obligation> userObsList = new ArrayList<Obligation>();
//	private List<Obligation> roleObsList = new ArrayList<Obligation>();
	
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
			List<Obligation> obls = parser.getObligation(); //get obligation list from the rule in policy.xml file
			
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
					
					if(obl.getActionName().contains(ObligationIds.REST_OBLIGATION_NAME_XML)){   //if the obligation is a rest obligation
						
						obligation = new RESTObligation((ObligationImpl)obl);			//create a new RESTObligation by raping this obligation
						
						((RESTObligation) obligation).setOpenmrsOblMonitor(openmrsOblMonitor);  //set the obligationMonitor for using it lately to update the budget. 
						
						obligation.setTriggeringUserId(currentUser.getId().toString());  //set the triggeringUserId to the currentUser id
						
						setOblSetId(obligation,setId);   //put the obligation to a specific set according to the setName attributes from the obligation
						
						if(attributeMap.containsKey("requiredUserId")){  //if the obligation is assigned to other user
							
							String userId = attributeMap.get("requiredUserId");  
							
							obligation.setUserId(userId); //set userId to the assigned user's Id, means this user will do the obligation
							
							//put this obligation to the userObs in context class
							if(SerContext.getUserObs().containsKey(userId)){  
								SerContext.getUserObs().get(userId).add(obligation);
							}else {
								List<Obligation> userObsList = new ArrayList<Obligation>();
								userObsList.add(obligation);
								SerContext.getUserObs().put(userId, userObsList);
							}
							
						}else if(attributeMap.containsKey("roleName")){  //if the obligation is assigned to a specific role
							
							//if it's a obligation to role, then don't need to set userId, just leave it null. 
							
							//put this obligation to the roleObs in context class
							String roleName = attributeMap.get("roleName");
							
							if(SerContext.getRoleObs().containsKey(roleName)){
								SerContext.getRoleObs().get(roleName).add(obligation);
							}else {
								List<Obligation> roleObsList = new ArrayList<Obligation>();
								roleObsList.add(obligation);
								SerContext.getRoleObs().put(roleName, roleObsList);
								}
						
						}else if(!attributeMap.containsKey("requiredUserId")&&!attributeMap.containsKey("roleName")){   //if the obligation not assigned to any role or any other user, which means the current user has to do the obligation
						
							obligation.setUserId(currentUser.getId().toString());  //currentUser id is the userId, and it is also the triggerringUserId
							
							if(SerContext.getUserObs().containsKey(currentUser.getId().toString())){
								SerContext.getUserObs().get(currentUser.getId().toString()).add(obligation);
							}else {
								List<Obligation> userObsList = new ArrayList<Obligation>();
								userObsList.add(obligation);
								SerContext.getUserObs().put(currentUser.getId().toString(), userObsList);
								}
							}
							
					}else{    //if the obligation is not a rest obligation, currently, we just defined rest obligation and email obligation,
						      //so we create a EmailObligation.
						obligation = new EmailObligation(obl.getActionName(),currentUser.getId().toString(),obl.getStartDate(),null);
						obligation.setAttributeMap(attributeMap);
						obligation.setUserId(currentUser.getId().toString());//userid and triggeringUiserId are the same.
						setOblSetId(obligation,setId);
					}
					
					obligation.setDecreasedBudget(decreasedBudget);	 //set the decreasedBudget to obligation
					
					SerContext.getActiveObs().put(obligation.getObUUID(), obligation); //put this obligation to activeObs in context class
					
					String message = obl.getAttribute("message") + "your UUID of the obligation is : "+ obligation.getObUUID().toString();
					messages.put(obl.getActionName(), message);
				}
			}
			System.err.println("the size of the activeObs is : " + SerContext.getActiveObs().size());
			System.err.println("the size of the userObs is : " + SerContext.getUserObs().size());
			System.err.println("the size of the roleObs is : " + SerContext.getRoleObs().size());
			System.err.println("the size of the oblSets is : " + SerContext.getObligationSets().size());
		}
		openmrsOblMonitor.checkObligations();   //check if obligation is being fulfilled or not 
		return messages;
	}

  
  public void updateBudget(String currentBudget){
	
	List<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
	attributes.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, currentBudget,
			StringAttribute.identifier));
	dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, currentUser.getId().toString(), attributes);
	
  }
  
  /**
   * put obligation to specific set according to the setName attribute from the obligation attributMap. 
   * if the obligation belongs to "budget_decrease_set", which means this obligation should be fulfilled
   * for getting the decreased budget back to the user, put the obligation to this set.
   * @param obligation
   * @param setId  the setId of the "budget_decrease_set"
   */
  public void setOblSetId(Obligation obligation, String setId){
	  
	  if(obligation.getAttribute("setName").contains(ObligationIds.BUDGET_DECREASE_SET_XML)){
			obligation.setSetId(setId);
			if(SerContext.getObligationSets().containsKey(setId)){
				SerContext.getObligationSets().get(setId).add(obligation); 
				System.err.println("Anita, the oblSets has contains this setId : " +setId+", and the "
						+ "there are " + SerContext.getObligationSets().get(setId).size() + "obligations with the same setId.");
			}else{
				List<Obligation> obls = new ArrayList<Obligation>();
				obls.add(obligation);
				SerContext.getObligationSets().put(setId,obls);
			}
		}else {
			//if the obligation dont belong to the "budget_decrease_set", create a new set, and add it to the hashmap in context class
			String newSetId = String.valueOf(UUID.randomUUID().getMostSignificantBits());
			obligation.setSetId(newSetId);
			List<Obligation> obls = new ArrayList<Obligation>();
			obls.add(obligation);
			SerContext.getObligationSets().put(newSetId,obls);
		}
  }

  /**
   * perform system obligations. Currently, system obligations including three types : show deny reason obligation, show permit
   * reason obligation and decrease budget obligation.
   */
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
