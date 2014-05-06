package org.openmrs.module.trumpmodule.obligations;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.wso2.balana.attr.StringAttribute;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.tmac.basic.data.xml.SubjectAttributeXmlName;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationIds;
import luca.tmac.basic.obligations.ObligationMonitorable;
import luca.tmac.basic.obligations.UserObligationMonitor;

public class OpenmrsUserObligationMonitor extends UserObligationMonitor {
	private Timer timer2;
	private Timer timer;
	private DataHandler dh = null;
//	private HashMap<String,Obligation> activeObs;
//	private HashMap<String,Obligation> fulfilledObs;
//	private HashMap<String,Obligation> expiredObs;
//	private HashMap<String,List<Obligation>> oblsSets;
	private OpenmrsEnforceServiceContext SerContext;
//	private HashMap<String, List<Obligation>> userObs;
//	private HashMap<String, List<Obligation>> roleObs;
	
	
	
	private ObligationMonitorable monitorableObject;

	public OpenmrsUserObligationMonitor(List<Obligation> oblList,
			ObligationMonitorable pm, DataHandler handler) {
		super(oblList, pm, handler);
		dh = handler;
		timer = new Timer();
		timer2 = new Timer();
		monitorableObject = pm;
		SerContext = OpenmrsEnforceServiceContext.getInstance();
		
//		activeObs = SerContext.getActiveObs();
//		fulfilledObs = SerContext.getFulfilledObs();
//		expiredObs = SerContext.getExpiredObs();
//		oblsSets = SerContext.getObligationSets();
		
	}
	
	public void checkObligations() {
		if(!SerContext.getActiveObs().isEmpty()){
			for(Entry<String, Obligation> e : SerContext.getActiveObs().entrySet()){
				Obligation ob = e.getValue();
				checkObligation(ob);
			}
		}
	}
	
	public void checkObligation(Obligation ob) {
	//	String actionName = ob.getActionName();
		
		//getAttributesFromDB();	
		//get the attributes of the obligation from data.xml file.
//		if(SerContext.getObsAttributes().containsKey(actionName)){
//			List<AttributeQuery> attributes = SerContext.getObsAttributes().get(actionName);
//		
//			//HashMap<String,String> attributeMap = new HashMap<String,String>();
//		
//			HashMap<String,String> attributeMap = ob.getAttributeMap();
//		
//			for(AttributeQuery aq : attributes)
//			{
//				attributeMap.put(aq.name, aq.value);
//			}
//	
//			ob.setAttributeMap(attributeMap);
//		}
		
		Date deadline = ob.getDeadline();
	
		//System.err.println("Anita ! the deadline of this obligation is : " + deadline);
		timer.schedule(new OblDeadlineTimerTask(ob, this), deadline);
		timer2.schedule(new OblCheckerTimerTask(ob, this) , 0 , 60000);
}
	
	
	/**
	 * this timer to check whether the obligation is fulfilled or not
	 * @author anitacao
	 *
	 */
	public class OblCheckerTimerTask extends TimerTask {
		String uuid;
		Obligation ob;
		OpenmrsUserObligationMonitor monitor;
		String userId;
		public OblCheckerTimerTask(Obligation ob,OpenmrsUserObligationMonitor monitor) {
			this.ob = ob;
			this.monitor = monitor;
		}
		  
		public void run() {
			if(SerContext.getActiveObs().containsValue(ob)){
			//if(activeObs.contains(ob)){
				String userId = ob.getUserId();
				//uuid = ob.getObUUID();
				uuid = ob.getObUUID();
				if(ob.isSatisfied(userId,uuid)){
					timer2.cancel();
					
					SerContext.getObligationSets().remove(ob.getSetId());
					
					if(!SerContext.getObligationSets().containsKey(ob.getSetId())){
						if(ob.getAttribute("setName").contains("budget_decrease_set")){
							String currentBudget = getBudgetfromDB(userId);
							String decreasedBudget = ob.getDecreasedBudget(); 
							
							//get currentBudget from database, plus the decreasedBudget stored in UserObRelation uo object, the result is the updatedBudget after fulfilling obligation 
							String newBudget = String
									.valueOf(Double.parseDouble(currentBudget) + Double.parseDouble(decreasedBudget));
							
							//modify data.xml file , update budget to database
							
							updateBudgetToDB(newBudget,userId);
							
							//check whether budget have been updated or not. 
							String updatedBudget = getBudgetfromDB(userId);
							System.err.println("Anita, the new budget get from database is : " + updatedBudget);
						}
					}
					
					
					//remove from the active list, add to the fulfilled list in openmrs enfroce service context. 
					SerContext.getActiveObs().remove(ob);
					SerContext.getFulfilledObs().put(uuid, ob);
					
					ob.getAttributeMap().put(Obligation.STATE_ATTRIBUTE_NAME, Obligation.STATE_FULFILLED);
					
					//activeObs.remove(ob);
					//fulfilledObs.put(uuid,ob);
					
//					SerContext.setActiveObs(activeObs);
//					SerContext.setFulfilledObs(fulfilledObs);
					
					if(ob.getAttributeMap().containsKey("requiredUserId")){
						
						SerContext.getUserObs().get(ob.getAttribute("requiredUserId")).remove(ob);
						
					}else if(ob.getAttributeMap().containsKey("roleName")){
						
						SerContext.getRoleObs().get(ob.getAttribute("roleName")).remove(ob);
						
					}
					
					System.err.println("Anita, now the size of activeObs is : " + SerContext.getActiveObs().size());
					System.err.println("Anita, now the size of fulfilledObs is : " + SerContext.getFulfilledObs().size());
					System.err.println("Anita, now the size of userObs is : " + SerContext.getUserObs().size());
					System.err.println("Anita, now the size of roleObs is : " + SerContext.getRoleObs().size());
					
					if(SerContext.getActiveObs().size()==0){
						System.err.println("Anita , good news : No more active obligation ! ");
					}
					
					monitorableObject.notifyFulfillment(ob);
				}
			}
		}
	}
	
	
	public class OblDeadlineTimerTask extends TimerTask {
		//String uuid;
		Obligation ob;
		OpenmrsUserObligationMonitor monitor;
		public OblDeadlineTimerTask(Obligation ob, OpenmrsUserObligationMonitor monitor){
			this.ob = ob;
			this.monitor = monitor;
		}
		
		public void run() {
			if(SerContext.getActiveObs().containsValue(ob)){
				timer.cancel();
				
				//remove from the active list, add to the expired list in openmrs enfroce service context. 
				SerContext.getActiveObs().remove(ob);
				SerContext.getExpiredObs().put(ob.getObUUID(),ob);
//				SerContext.setActiveObs(activeObs);
//				SerContext.setExpiredObs(expiredObs);
				ob.getAttributeMap().put(Obligation.STATE_ATTRIBUTE_NAME, Obligation.STATE_EXPIRED);
				monitorableObject.notifyDeadline(ob);
			}
		}
	}
	
	
//	public void getAttributesFromDB() {
//		HashMap<String,List<AttributeQuery>> obsAttributes = new HashMap<String,List<AttributeQuery>>();
//		List<String> ids = dh.getAttribute("obligation", new ArrayList<AttributeQuery>(), "id");
//
//		for (String oblId : ids) {
//
//			List<AttributeQuery> attributes = dh.getAttributesOf("obligation",
//					oblId);
//			String actionName = null;
//			for (AttributeQuery att : attributes) {
//				if (att.name
//						.equals(ObligationIds.ACTION_NAME_OBLIGATION_ATTRIBUTE)) {
//					actionName = att.value;
//				}
//			}
//			obsAttributes.put(actionName, attributes);
//			SerContext.setObsAttributes(obsAttributes);
//		}
//	}
	
	public String getBudgetfromDB(String userId){
		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		query.add(new AttributeQuery(SubjectAttributeXmlName.ID, userId,StringAttribute.identifier));
		List<String> budgets = dh.getAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query, "budget");
		return budgets.get(0);
	}
	
	public void updateBudgetToDB(String currentBudget,String userId){
			
		List<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
		attributes.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, currentBudget,
				StringAttribute.identifier));
		dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, userId, attributes);
			
	}
	
	public void updateBudget(){
		
	}

}		
	

	
	

