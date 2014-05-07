package org.openmrs.module.trumpmodule.obligations;

import java.util.ArrayList;
import java.util.Date;
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
import luca.tmac.basic.obligations.ObligationMonitorable;
import luca.tmac.basic.obligations.UserObligationMonitor;

public class OpenmrsUserObligationMonitor extends UserObligationMonitor {
	private Timer timer2;
	private Timer timer;
	private DataHandler dh = null;
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
		
	}
	
	public void checkObligations() {
		if(!SerContext.getActiveObs().isEmpty()){
			
			for(Entry<String, Obligation> e : SerContext.getActiveObs().entrySet()){
				Obligation ob = e.getValue();
				
				if(ob instanceof EmailObligation)
					timer2.schedule(new OblCheckerTimerTask(ob, this) , 0 , 60000);
				
				checkObligation(ob);
			}
		}
	}
	
	public void checkObligation(Obligation ob) {
		
		Date deadline = ob.getDeadline();
		timer.schedule(new OblDeadlineTimerTask(ob, this), deadline);
		
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
				String triggeringUserId = ob.getTriggeringUserId();
				//uuid = ob.getObUUID();
				uuid = ob.getObUUID();
				if(ob.isSatisfied(triggeringUserId,uuid)){
					timer2.cancel();
					
					SerContext.getObligationSets().remove(ob.getSetId());
					updateBudget(ob, triggeringUserId);
					
					//remove from the active list, add to the fulfilled list in openmrs enfroce service context. 
					SerContext.getActiveObs().remove(ob);
					SerContext.getFulfilledObs().put(uuid, ob);
					
					ob.getAttributeMap().put(Obligation.STATE_ATTRIBUTE_NAME, Obligation.STATE_FULFILLED);
					
					
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
				ob.getAttributeMap().put(Obligation.STATE_ATTRIBUTE_NAME, Obligation.STATE_EXPIRED);
				monitorableObject.notifyDeadline(ob);
			}
		}
	}
	
	
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
	
	public void updateBudget(Obligation ob, String triggeringUserId){
		if(!SerContext.getObligationSets().containsKey(ob.getSetId())){
			if(ob.getAttribute("setName").contains("budget_decrease_set")){
				
				String currentBudget = getBudgetfromDB(triggeringUserId);
				String decreasedBudget = ob.getDecreasedBudget(); 
				
				//get currentBudget from database, plus the decreasedBudget stored in UserObRelation uo object, the result is the updatedBudget after fulfilling obligation 
				String newBudget = String
						.valueOf(Double.parseDouble(currentBudget) + Double.parseDouble(decreasedBudget));
				
				//modify data.xml file , update budget to database
				updateBudgetToDB(newBudget,triggeringUserId);
				
				//check whether budget have been updated or not. 
				String updatedBudget = getBudgetfromDB(triggeringUserId);
				System.err.println("Anita, the new budget get from database is : " + updatedBudget);
			}
		}
	}

}		
	

	
	

