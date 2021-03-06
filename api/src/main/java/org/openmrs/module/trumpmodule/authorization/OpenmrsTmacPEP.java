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
import org.wso2.balana.XACMLConstants;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.tmac.basic.ResponseParser;
import luca.tmac.basic.TmacPDP;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.uris.ActionAttributeURI;
import luca.tmac.basic.data.uris.PermissionAttributeURI;
import luca.tmac.basic.data.uris.RiskAttributeURI;
import luca.tmac.basic.data.uris.SubjectAttributeURI;
import luca.tmac.basic.data.uris.TaskAttributeURI;
import luca.tmac.basic.data.uris.TeamAttributeURI;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationIds;
import luca.tmac.basic.obligations.ObligationImpl;
import luca.tmac.basic.obligations.ObligationMonitorable;
import luca.tmac.basic.obligations.UserObligationMonitor;

public class OpenmrsTmacPEP  {
	
	private OpenmrsUserObligationMonitor openmrsOblMonitor;
	private User currentUser = Context.getAuthenticatedUser(); //currentUser is the triggering user of the obligation
	private HashMap<String,String> messages;
	
	public TmacPDP pdp;
	public UserObligationMonitor obligationMonitor;
	public DataHandler dh;
	public HashMap<Long, ResponseParser> sessionParsers;
	public ObligationMonitorable monitorable;

	OpenmrsEnforceServiceContext SerContext = OpenmrsEnforceServiceContext.getInstance();
	
	public OpenmrsTmacPEP(DataHandler parDataHandler,
			ObligationMonitorable monitorable) {
		this.monitorable = monitorable;
		dh = parDataHandler;
		pdp = new TmacPDP(dh, getTopLevelPolicyDirectory(),  getUserPolicyDirectory());
		
		sessionParsers = new HashMap<Long, ResponseParser>();
		
		openmrsOblMonitor = new OpenmrsUserObligationMonitor(new ArrayList<Obligation>(),monitorable,dh);
	}
	
	
	public String getUserPolicyDirectory() {
		return OpenmrsEnforceServiceContext.getInstance().getUserPolicyDirectory();
	}

	
	public String getTopLevelPolicyDirectory() {
		return OpenmrsEnforceServiceContext.getInstance().getTopLevelPolicyDirectory();
	}
	
	/**
	 * Register a new attribute finder module with the PDP associated with this PEP.
	 * @param m - the attribute finder module.
	 */
	public void addAttributeFinderToPDP(AbstractAttributeFinderModule m)
	{
		pdp.addFinderModule(m);
	}
	
	public void createPDP(){
		pdp.createPDP();
	}

	public HashMap<String,String> acceptResponse(long parserId,String methodName) {
		ResponseParser parser = sessionParsers.get(parserId);
		messages = new HashMap<String,String>();
		String budgetfromDb = openmrsOblMonitor.getBudgetfromDB(currentUser.getId().toString());
		double previousBudget = Double.parseDouble(budgetfromDb);
		if (parser == null)
			throw new IllegalArgumentException("invalid parser id");
		
		//if(methodName.equalsIgnoreCase("getPatientByUuid")){
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
					
					//if the obligation is a rest obligation
					if(obl.getActionName().contains(ObligationIds.REST_OBLIGATION_NAME_XML)){   
						
						//create a new RESTObligation by raping this obligation
						obligation = new RESTObligation((ObligationImpl)obl);			
						
						//set the obligationMonitor for using it lately to update the budget.
						((RESTObligation) obligation).setOpenmrsOblMonitor(openmrsOblMonitor);   
						
						//set the triggeringUserId to the currentUser id
						obligation.setTriggeringUserId(currentUser.getId().toString());  
						
						//put the obligation to a specific set according to the setName attributes from the obligation
						setOblSetId(obligation,setId);   
						
						//if the obligation is assigned to other user
						if(attributeMap.containsKey("requiredUserId")){  
							
							String userId = attributeMap.get("requiredUserId");  
							
							//set userId to the assigned user's Id, means this user will do the obligation
							obligation.setUserId(userId); 
							
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
						
							 
						}else 
							//if the obligation not assigned to any role or any other user, which means the current user has to do the obligation
							if(!attributeMap.containsKey("requiredUserId")&&!attributeMap.containsKey("roleName")){
						
							//currentUser id is the userId, and it is also the triggerringUserId
							obligation.setUserId(currentUser.getId().toString());  
							
							if(SerContext.getUserObs().containsKey(currentUser.getId().toString())){
								SerContext.getUserObs().get(currentUser.getId().toString()).add(obligation);
							}else {
								List<Obligation> userObsList = new ArrayList<Obligation>();
								userObsList.add(obligation);
								SerContext.getUserObs().put(currentUser.getId().toString(), userObsList);
								}
							}
							
					}else{  //if the obligation is not a rest obligation, currently, we just defined rest obligation and email obligation,
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
		//}
		//check if obligation has been fulfilled or not 
		openmrsOblMonitor.checkObligations();   
		return messages;
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
			//if the obligation dont belong to the "budget_decrease_set", create a new set, and add it to the hash map in context class
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
	
	public String createXACMLRequest(String username, String permission,
			String team, String task, String requestType) {

		String subjectCategory = "";
		String permissionCategory = "";
		String teamCategory = "";
		String taskCategory = "";
		String actionCategory = "";
		String envCategory = "";

		if (username != null && !username.equals(""))
			subjectCategory = "<Attributes Category=\""
					+ SubjectAttributeURI.SUBJECT_CATEGORY_URI
					+ "\">\n"
					+ "<Attribute AttributeId=\""
					+ SubjectAttributeURI.ID_URI
					+ "\" IncludeInResult=\"false\">\n"
					+ "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"
					+ username + "</AttributeValue>\n" + "</Attribute>\n"
					+ "</Attributes>\n";

//		if (permission != null && !permission.equals(""))
//			permissionCategory = "<Attributes Category=\""
//					+ PermissionAttributeURI.PERMISSION_CATEGORY_URI
//					+ "\">\n"
//					+ "<Attribute AttributeId=\""
//					+ PermissionAttributeURI.PERMISSION_ID_URI
//					+ "\" IncludeInResult=\"false\">\n"
//					+ "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"
//					+ permission + "</AttributeValue>\n" + "</Attribute>\n"
//					+ "</Attributes>\n";
		if (permission != null )
		permissionCategory = "<Attributes Category=\""
				+ PermissionAttributeURI.PERMISSION_CATEGORY_URI
				+ "\">\n"
				+ "<Attribute AttributeId=\""
				+ PermissionAttributeURI.PERMISSION_ATTRIBUTES_URI
				+ "\" IncludeInResult=\"false\">\n"
				+ "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"
				+ permission + "</AttributeValue>\n" + "</Attribute>\n"
				+ "</Attributes>\n";
		
		if (team != null && !team.equals(""))
			teamCategory = "<Attributes Category=\""
					+ TeamAttributeURI.TEAM_CATEGORY_URI
					+ "\">\n"
					+ "<Attribute AttributeId=\""
					+ TeamAttributeURI.ID_URI
					+ "\" IncludeInResult=\"false\">\n"
					+ "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"
					+ team + "</AttributeValue>\n" + "</Attribute>\n"
					+ "</Attributes>\n";

		if (task != null && !task.equals(""))
			taskCategory = "<Attributes Category=\""
					+ TaskAttributeURI.TASK_CATEGORY_URI
					+ "\">\n"
					+ "<Attribute AttributeId=\""
					+ TaskAttributeURI.ID_URI
					+ "\" IncludeInResult=\"false\">\n"
					+ "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"
					+ task + "</AttributeValue>\n" + "</Attribute>\n"
					+ "</Attributes>\n";

		if (requestType != null && !requestType.equals(""))
			actionCategory = "<Attributes Category=\""
					+ ActionAttributeURI.ACTION_CATEGORY_URI
					+ "\">\n"
					+ "<Attribute AttributeId=\""
					+ ActionAttributeURI.ACTION_ID_URI
					+ "\" IncludeInResult=\"false\">\n"
					+ "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">"
					+ requestType + "</AttributeValue>\n" + "</Attribute>\n"
					+ "</Attributes>\n";

		envCategory = "<Attributes Category=\"" + XACMLConstants.ENT_CATEGORY
				+ "\">\n" + "</Attributes>\n";

		String riskCategory = "<Attributes Category=\""
				+ RiskAttributeURI.RISK_CATEGORY_URI + "\">\n"
				+ "</Attributes>\n";

		String request = "<Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"false\">\n"
				+ subjectCategory
				+ permissionCategory
				+ teamCategory
				+ taskCategory
				+ actionCategory
				+ envCategory
				+ riskCategory
				+ "</Request>";

		return request;
	}
	
//	public ResponseParser requestAccess(String username, String permission,
//			String team, String task, String requestType) {
//		String request = createXACMLRequest(username, permission, team, task,
//				requestType);
//		String response = pdp.evaluate(request);
//		ResponseParser rParser = new ResponseParser(response,dh);
//		System.out.println("Anita , 2 the decision is : " + rParser.getDecision());
//		if(rParser.getDecision().equalsIgnoreCase("Permit"))
//				sessionParsers.put(rParser.getParserId(), rParser);
//		return rParser;
//	}
	public ResponseParser requestAccess(String username, String permissionString,
			String team, String task, String requestType) {
		String request = createXACMLRequest(username, permissionString, team, task,
				requestType);
		String response = pdp.evaluate(request);
		ResponseParser rParser = new ResponseParser(response,dh);
		System.out.println("Anita , 2 the decision is : " + rParser.getDecision());
		if(rParser.getDecision().equalsIgnoreCase("Permit"))
				sessionParsers.put(rParser.getParserId(), rParser);
		return rParser;
	}
}
