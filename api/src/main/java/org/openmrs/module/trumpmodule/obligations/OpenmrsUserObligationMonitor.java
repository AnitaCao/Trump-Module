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
	private HashMap<String,Obligation> activeObs;
	private HashMap<String,Obligation> fulfilledObs;
	private HashMap<String,Obligation> expiredObs;
	private OpenmrsEnforceServiceContext SerContext;
	
	
	
	private ObligationMonitorable monitorableObject;

	public OpenmrsUserObligationMonitor(List<Obligation> oblList,
			ObligationMonitorable pm, DataHandler handler) {
		super(oblList, pm, handler);
		dh = handler;
		timer = new Timer();
		timer2 = new Timer();
		monitorableObject = pm;
		SerContext = OpenmrsEnforceServiceContext.getInstance();
		
		activeObs = SerContext.getActiveObs();
		fulfilledObs = SerContext.getFulfilledObs();
		expiredObs = SerContext.getExpiredObs();
		
	}
	
	public void checkObligations() {
		if(!activeObs.isEmpty()){
			for(Entry<String, Obligation> e : activeObs.entrySet()){
				Obligation ob = e.getValue();
				checkObligation(ob);
			}
		}
	}
	
	public void checkObligation(Obligation ob) {
		String actionName = ob.getActionName();
		
		getAttributesFromDb();	
		List<AttributeQuery> attributes = SerContext.getObsAttributes().get(actionName);
		HashMap<String,String> attributeMap = new HashMap<String,String>();
		for(AttributeQuery aq : attributes)
		{
			attributeMap.put(aq.name, aq.value);
		}
	
		ob.setAttributeMap(attributeMap);
		Date deadline = ob.getDeadline();
	
		System.err.println("Anita ! the deadline of this obligation is : " + deadline);
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
			if(activeObs.containsValue(ob)){
			//if(activeObs.contains(ob)){
				String userId = ob.getUserId();
				//uuid = ob.getObUUID();
				uuid = ob.getObUUID();
				if(ob.isSatisfied(userId,uuid)){
					timer2.cancel();
					String currentBudget = getBudgetfromDb(userId);
					String decreasedBudget = ob.getDecreasedBudget(); 
					
					//get currentBudget from database, plus the decreasedBudget stored in UserObRelation uo object, the result is the updatedBudget after fulfilling obligation 
					String newBudget = String
							.valueOf(Double.parseDouble(currentBudget) + Double.parseDouble(decreasedBudget));
					
					//modify data.xml file , update budget to database
					
					updateBudget(newBudget,userId);
					
					//check whether budget have been updated or not. 
					String updatedBudget = getBudgetfromDb(userId);
					System.err.println("Anita, the new budget get from database is : " + updatedBudget);
					
					//remove from the active list, add to the fulfilled list in openmrs enfroce service context. 
					activeObs.remove(ob);
					fulfilledObs.put(uuid,ob);
					SerContext.setActiveObs(activeObs);
					SerContext.setFulfilledObs(fulfilledObs);
					System.err.println("Anita, now the active obligation is : " + activeObs.size());
					System.err.println("Anita, now the fulfilled obligation is : " + fulfilledObs.size());
					if(activeObs.size()==0){
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
			if(activeObs.containsValue(ob)){
				timer.cancel();
				
				//remove from the active list, add to the expired list in openmrs enfroce service context. 
				activeObs.remove(ob);
				expiredObs.put(ob.getObUUID(),ob);
				SerContext.setActiveObs(activeObs);
				SerContext.setExpiredObs(expiredObs);
				monitorableObject.notifyDeadline(ob);
			}
		}
	}
	
	public void getAttributesFromDb() {
		HashMap<String,List<AttributeQuery>> obsAttributes = new HashMap<String,List<AttributeQuery>>();
		List<String> ids = dh.getAttribute("obligation", new ArrayList<AttributeQuery>(), "id");

		for (String oblId : ids) {

			List<AttributeQuery> attributes = dh.getAttributesOf("obligation",
					oblId);
			String actionName = null;
			for (AttributeQuery att : attributes) {
				if (att.name
						.equals(ObligationIds.ACTION_NAME_OBLIGATION_ATTRIBUTE)) {
					actionName = att.value;
				}
			}
			obsAttributes.put(actionName, attributes);
			SerContext.setObsAttributes(obsAttributes);
		}
	}
	
	public String getBudgetfromDb(String userId){
		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		query.add(new AttributeQuery(SubjectAttributeXmlName.ID, userId,StringAttribute.identifier));
		List<String> budgets = dh.getAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query, "budget");
		return budgets.get(0);
	}
	
	public void updateBudget(String currentBudget,String userId){
			
		List<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
		attributes.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, currentBudget,
				StringAttribute.identifier));
		dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, userId, attributes);
			
	}
}		
	

	
	

