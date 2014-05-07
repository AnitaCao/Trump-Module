package luca.tmac.basic;

import java.util.HashMap;

import org.wso2.balana.XACMLConstants;

import luca.data.DataHandler;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.uris.ActionAttributeURI;
import luca.tmac.basic.data.uris.PermissionAttributeURI;
import luca.tmac.basic.data.uris.RiskAttributeURI;
import luca.tmac.basic.data.uris.SubjectAttributeURI;
import luca.tmac.basic.data.uris.TaskAttributeURI;
import luca.tmac.basic.data.uris.TeamAttributeURI;
import luca.tmac.basic.obligations.ObligationIds;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationMonitorable;
import luca.tmac.basic.obligations.UserObligationMonitor;

public class TmacPEP {

	public static final String TOP_LEVEL_POLICIES_PATH = "./tmac/basic/top-policies";
	public static final String OTHER_POLICIES_PATH = "./tmac/basic/others-policies";

	public TmacPDP pdp;
	public UserObligationMonitor obligationMonitor;
	public DataHandler dh;
	public HashMap<Long, ResponseParser> sessionParsers;
	public ObligationMonitorable monitorable;

	
	public TmacPEP(DataHandler parDataHandler,ObligationMonitorable monitorable) {
		
		this.monitorable = monitorable;
		dh = parDataHandler;
		pdp = new TmacPDP(dh, getTopLevelPolicyDirectory(),  getUserPolicyDirectory());
		
		sessionParsers = new HashMap<Long, ResponseParser>();
	}
	
	public String getUserPolicyDirectory() {
		return OTHER_POLICIES_PATH;
	}
	public String getTopLevelPolicyDirectory() {
		return TOP_LEVEL_POLICIES_PATH;
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
	
	public ResponseParser requestAccess(String username, String permission,
			String team, String task, String requestType) {
		String request = createXACMLRequest(username, permission, team, task,
				requestType);
		String response = pdp.evaluate(request);
		ResponseParser rParser = new ResponseParser(response,dh);
		System.out.println("Anita , 2 the decision is : " + rParser.getDecision());
		if(rParser.getDecision().equalsIgnoreCase("Permit"))
				sessionParsers.put(rParser.getParserId(), rParser);
		return rParser;
	}

	public HashMap<String,String> acceptResponse(long parserId) {
		ResponseParser parser = sessionParsers.get(parserId);
		HashMap<String,String> messages = new HashMap<String,String>();
	//	ObligationSet oblSet = new ObligationSet(new ArrayList<Obligation>(),dh);
		if (parser == null)
			throw new IllegalArgumentException("invalid parser id");
		for (Obligation obl : parser.getObligation()) {
			
			if (obl.isSystemObligation()) {
				String message = performObligation(obl);
				messages.put(obl.getActionName(), message);
				//message += performObligation(obl) +"\n";
			} else {
				//if it's not system obligation which means it's a user obligation
				//oblSet.add(obl); //add user obligation to obligation set. 
				obligationMonitor.addObligation(obl); //add obligation to database
			}
		}
		return messages;
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

		if (permission != null && !permission.equals(""))
			permissionCategory = "<Attributes Category=\""
					+ PermissionAttributeURI.PERMISSION_CATEGORY_URI
					+ "\">\n"
					+ "<Attribute AttributeId=\""
					+ PermissionAttributeURI.PERMISSION_ID_URI
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
	
	/**
	 * perform system obligation.
	 * @param obl
	 * @return
	 */
	public String performObligation(Obligation obl)
	{
		if(obl.getActionName().equals(ObligationIds.SHOW_DENY_REASON_OBLIGATION_ID))
		{
			String message = obl.getAttribute("message");
			return message;
			
		}else if(obl.getActionName().equals(ObligationIds.DECREASE_BUDGET_ID)){
			//String message = obl.getAttribute("budgetDecreaseMessage");
			
			double neededBudget = Double.parseDouble(obl.getAttribute("needed-budget"));
			double budget = Double.parseDouble(obl.getAttribute("budget"));
			String currentBudgetString = Double.toString(budget-neededBudget);
			
			//return currentBudgetString + ","+ obl.getAttribute("needed-budget");  //return the currentBudget for updating the budget, return the needed budget for increasing back the
			return currentBudgetString;																  //budget after the obligation being performed.
			
		}
		else throw new IllegalArgumentException("system obligation not supported:" + obl.getActionName());
	}
	

}
