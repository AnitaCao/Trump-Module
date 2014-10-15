package org.openmrs.module.trumpmodule.authorization;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.tmac.basic.ResponseParser;
import luca.tmac.basic.StandardBudgetCalculator;
import luca.tmac.basic.data.xml.PermissionAttributeXmlName;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationMonitorable;



//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.TmacEnforceService;
import org.openmrs.module.trumpmodule.dataFinder.*;
import org.wso2.balana.attr.StringAttribute;


public class TmacEnforceServiceImpl extends BaseOpenmrsService implements TmacEnforceService,ObligationMonitorable {
	
	
	//private static final Log LOG = LogFactory.getLog(TmacEnforceServiceImpl.class);

	//private static final long serialVersionUID = -8561161513239681330L;
	private OpenmrsTmacPEP pep;
	private DataHandler dh;
	private long responseParserId;
	
	public static final int ALLOW_NO_SUCH_PERMISSION = 1;
	public static final int ALLOW = 2;
	public static final int DENY = 3;
	
	String userID;
	
	//messages stores the system obligation performing result messages. 
	HashMap<String,String> messages = null;
	
	public TmacEnforceServiceImpl(Object[] parameters,String methodName) throws FileNotFoundException {
		
		dh = OpenmrsEnforceServiceContext.getInstance().getDh();
		pep = new OpenmrsTmacPEP(dh,this);
		
		//add new subject attribute finder and risk attribute finder to PEP
		pep.addAttributeFinderToPDP(new OpenmrsSubjectAttributeFinderModule((dh),parameters,methodName));
		pep.addAttributeFinderToPDP(new OpenmrsRiskAttributeFinderModule(dh,new StandardBudgetCalculator()));
		pep.addAttributeFinderToPDP(new OpenmrsPermissionAttributeFinderModule((dh),parameters,methodName));
		pep.createPDP();
	}
	
	/**
	 * create and send request to pdp
	 * @param privilege a string which contains the action and resource
	 * @param user
	 * @return boolean
	 */
	@Override
	public int isAuthorized(String privilege, User user) {
    	System.out.println( "Anita !!!!!!!!!!!!!!!!!! the required privilege is : " + privilege + ""
    			+ "userId is : " + user.getId().toString());
    	
//    	String[] parms = privilege.split(" ");
//    	String actionString = parms[0].toLowerCase();
//    	String resourceString = parms[1].toLowerCase();
    	
    	//set all the attributes
    	//ArrayList<AttributeQuery> permissionAttributeQuery = new ArrayList<AttributeQuery>();
    	
    	//String permission = "";
		//List<String> permission_ids = null;
//		permissionAttributeQuery.clear();
//		permissionAttributeQuery.add(new AttributeQuery(
//				PermissionAttributeXmlName.ACTION, actionString,
//				StringAttribute.identifier));
//		permissionAttributeQuery.add(new AttributeQuery(
//				PermissionAttributeXmlName.RESOURCE_TYPE, resourceString,   
//				StringAttribute.identifier));
//		permission_ids = dh.getAttribute(
//				PermissionAttributeXmlName.PERMISSION_TABLE,
//				attributeQuery, PermissionAttributeXmlName.ID);
//		
//		if (permission_ids == null || permission_ids.size() == 0) {
//			
//			return ALLOW_NO_SUCH_PERMISSION;
//		}
//		permission = permission_ids.get(0);
		
		
		//the request type is "obtain_permission"
		userID = user.getId().toString();

		ResponseParser rParser = pep.requestAccess(userID,
				privilege, "", "",
				"obtain_permission");

		responseParserId = rParser.getParserId();
		
		//if the decision is permit, return true
		if(rParser.getDecision().equals(ResponseParser.PERMIT_RESPONSE)){	
			return ALLOW;
		}
		else{  
			//if the decision is not permit, get the obligations from the rule in policy.xml file(if there is any obligation),
			//perform the system obligations.
			String message = "";
			for (Obligation obl : rParser.getObligation()) {
				if (obl.isSystemObligation())
					message += pep.performObligation(obl) + "\n";
			}
			System.err.println("the deny reason (from system obligation ) is  : " + message );
			return DENY;
		}
    }
    
    public HashMap<String,String> acceptResponse(String methodName){
    	HashMap<String, String> messages = pep.acceptResponse(responseParserId,methodName);
    	
    	return messages;
    }
    


	public void notifyDeadline(Obligation obl) {
		obl.getAttributeMap().put(Obligation.STATE_ATTRIBUTE_NAME, Obligation.STATE_EXPIRED);
		
	}


	public void notifyFulfillment(Obligation obl) {
		obl.getAttributeMap().put(Obligation.STATE_ATTRIBUTE_NAME, Obligation.STATE_FULFILLED);
		
	}


	public void notifyObligationInsert(Obligation obl) {
		
	}


	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onStartup() {
		// TODO Auto-generated method stub
		
	}





}
