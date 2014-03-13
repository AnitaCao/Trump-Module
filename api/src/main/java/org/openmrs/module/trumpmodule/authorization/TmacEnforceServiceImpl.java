package org.openmrs.module.trumpmodule.authorization;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.data.XmlDataHandler;
import luca.tmac.basic.ResponseParser;
import luca.tmac.basic.StandardBudgetCalculator;
import luca.tmac.basic.data.xml.PermissionAttributeXmlName;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationMonitorable;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.module.trumpmodule.TmacEnforceService;
import org.openmrs.module.trumpmodule.dataFinder.OpenmrsRiskAttributeFinderModule;
import org.openmrs.module.trumpmodule.dataFinder.OpenmrsSubjectAttributeFinderModule;
import org.wso2.balana.attr.StringAttribute;


public class TmacEnforceServiceImpl implements TmacEnforceService,ObligationMonitorable {
	
	
	//private static final Log LOG = LogFactory.getLog(TmacEnforceServiceImpl.class);
	private static final String RESOURCE_PATH = "data.xml";

	//private static final long serialVersionUID = -8561161513239681330L;
	private OpenmrsTmacPEP pep;
	private DataHandler dh;
	//private List<String> obligationList;
	private long responseParserId;
	
	String userID;
	
	//messages stores the system obligation performing result messages. 
	HashMap<String,String> messages = null;
	
	public TmacEnforceServiceImpl(Object[] parameters) throws FileNotFoundException {
		
		String path = this.getClass().getClassLoader().getResource(RESOURCE_PATH).toString().substring(5);
		//BufferedReader path2 = new BufferedReader(new FileReader(RESOURCE_PATH));
		//System.err.println("SERIOUSLY CHRIS : " + path);
		dh = new XmlDataHandler(path);
		
		pep = new OpenmrsTmacPEP(dh,this);
		//pep = new TmacPEP(dh,this);
		//pep.createTmacPDP(TOP_LEVEL_POLICIES_FOLDER ,OTHER_POLICIES_FOLDER);
		pep.addAttributeFinderToPDP(new OpenmrsSubjectAttributeFinderModule((dh),parameters));
		pep.addAttributeFinderToPDP(new OpenmrsRiskAttributeFinderModule(dh,new StandardBudgetCalculator()));
		pep.createPDP();
		
	}


	public boolean isAuthorized(String priviledge, User user) throws APIException {
		
		boolean isAuthorized = false;
		isAuthorized = sendRequest(priviledge, user);	
		return isAuthorized;
	}
	
	/**
	 * create and send request to pdp
	 * @param privilege a string which contains the action and resource
	 * @param user
	 * @return
	 */
    public boolean sendRequest(String privilege, User user){
    	
    	System.out.println( "Anita !!!!!!!!!!!!!!!!!! the required privilege is : " + privilege);
    	
    	String[] parms = privilege.split(" ");
    	String actionString = parms[0].toLowerCase();
    	String resourceString = parms[1].toLowerCase();
    	
    	//set all the attributes
    	ArrayList<AttributeQuery> attributeQuery = new ArrayList<AttributeQuery>();
    	String permission = "";
		List<String> permission_ids = null;
		attributeQuery.clear();
		attributeQuery.add(new AttributeQuery(
				PermissionAttributeXmlName.ACTION, actionString,
				StringAttribute.identifier));
		attributeQuery.add(new AttributeQuery(
				PermissionAttributeXmlName.RESOURCE_TYPE, resourceString,   
				StringAttribute.identifier));
		permission_ids = dh.getAttribute(
				PermissionAttributeXmlName.PERMISSION_TABLE,
				attributeQuery, PermissionAttributeXmlName.ID);
		
		if (permission_ids == null || permission_ids.size() == 0) {
			
			System.out.println("Anita ! the persission is empty : " + permission_ids.size());
			
			return false;
		}
		permission = permission_ids.get(0);
		
		//the request type is "obtain_permission"
		userID = user.getId().toString();
		ResponseParser rParser = pep.requestAccess(userID,
				permission, "", "",
				"obtain_permission");

		responseParserId = rParser.getParserId();
		//List<Obligation> oblList = rParser.getObligation().getList();
		
//		String oblText = "";
//		for (Obligation obl : oblList) {
//			oblText += obl.toString() + "\n";
//		}
//		
//		System.err.println("Anita, the obligation get from the policy here is : " + oblText +", the number of obligation is : " + oblList.size());
		
		//if the decision is permit, return true
		if(rParser.getDecision().equals(ResponseParser.PERMIT_RESPONSE)){	
			//assume automatically accept response 
			//messages = pep.acceptResponse(responseParserId);
			return true;
		}
		else{
			String message = "";
			for (Obligation obl : rParser.getObligation().getList()) {
				if (obl.isSystemObligation())
					message += pep.performObligation(obl) + "\n";
			}
			System.err.println(message + "the system obligation is this message");
			return false;
		}
    }
    
    
    public HashMap<String,String> acceptResponse(String methodName){
    	HashMap<String, String> messages = pep.acceptResponse(responseParserId,methodName);
    	return messages;
    }
    
    /**
     * get user obligation list 
     * @return List<Obligation> 
     */
    public List<Obligation> getObligationList(){
    	List<Obligation> obList = pep.obligationMonitor.getList();
    	return obList;
    }
    
    /**
     * update the budget in the data.xml file to currentBudget
     * @param currentBudget
     */
//    public void updateBudget(String currentBudget){
//    	
//    	List<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
//    	attributes.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, currentBudget,
//				StringAttribute.identifier));
//    	
//    	dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, userID, attributes);
//    }
//    
//   public String updateBudget(String currentBudget){
//    	String previousBudget = getBudgetfromDb();
//    	List<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
//    	attributes.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, currentBudget,
//				StringAttribute.identifier));
//    	
//    	dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, userID, attributes);
//    	return previousBudget;
//    } 
    
    /**
     * get budget from database(it's the same as the subject finder module)
     * @return
     */
//    public String getBudgetfromDb(){
//    	ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
//		query.add(new AttributeQuery(SubjectAttributeXmlName.ID, userID,StringAttribute.identifier));
//    	List<String> budgets = dh.getAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query, "budget");
//    	return budgets.get(0);
//    }
    
    
    public void fulfillObligation(Obligation obl){
    	pep.fulfillObligation(obl);
    }
   
	public void notifyDeadline(Obligation obl) {
		// TODO Auto-generated method stub
		
	}

	public void notifyFulfillment(Obligation obl) {
		// TODO Auto-generated method stub
		
	}

	public void notifyObligationInsert(Obligation obl) {
		// TODO Auto-generated method stub
		
	}

	public List<String> getObligations() throws APIException {
		// TODO Auto-generated method stub
		return null;
	}



}
