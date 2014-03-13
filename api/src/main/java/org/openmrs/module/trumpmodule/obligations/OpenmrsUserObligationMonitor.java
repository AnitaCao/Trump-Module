package org.openmrs.module.trumpmodule.obligations;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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
	private HashMap<UUID, UserObRelation> activeObs;
	private HashMap<UUID, UserObRelation> fulfilledObs;
	private HashMap<UUID, UserObRelation> expiredObs;
	
	
	private ObligationMonitorable monitorableObject;

	public OpenmrsUserObligationMonitor(List<Obligation> oblList,
			ObligationMonitorable pm, DataHandler handler) {
		super(oblList, pm, handler);
		dh = handler;
		timer = new Timer();
		timer2 = new Timer();
		monitorableObject = pm;
		
		activeObs = OpenmrsEnforceServiceContext.getActiveOb();
		fulfilledObs = OpenmrsEnforceServiceContext.getFulfilledOb();
		expiredObs = OpenmrsEnforceServiceContext.getExpiredOb();
		
//		if(!activeObs.isEmpty()){
//			checkObligations();
//		}
	}
	
	public void checkObligations() {
		if(!activeObs.isEmpty()){
			for(UUID uuid : activeObs.keySet()){
				checkObligation(uuid);
			}
		}
	}
	
	public void checkObligation(UUID uuid) {
		UserObRelation uo = activeObs.get(uuid);
		String oblId = uo.obId;
		Date startDate = uo.date;
		Obligation obl = getObfromDB(oblId,startDate);
	
		Date deadline = obl.getDeadline();
		System.out.println("Anita ! the deadline is : " + deadline);
		timer.schedule(new OblDeadlineTimerTask(uuid, this), deadline);
			
		timer2.schedule(new OblCheckerTimerTask(uuid, this) , 0 , 60000);
		
	}
	
	/**
	 * get obligation from database according to the obligation id. 
	 * @param oblId
	 * @return obl Obligation 
	 */
	public Obligation getObfromDB(String oblId,Date startTime){
		Obligation obl = null;
		List<AttributeQuery> attributes = dh.getAttributesOf("obligation",
				oblId);
		ArrayList<AttributeQuery> newAttList = new ArrayList<AttributeQuery>();
		String actionName = null;
		for (AttributeQuery att : attributes) {
			if (att.name
					.equals(ObligationIds.ACTION_NAME_OBLIGATION_ATTRIBUTE)) {
				actionName = att.value;
			} else {
				newAttList.add(att);
			}
		}
		if(actionName.equals(EmailObligation.obligationNameXML)){
			obl = new EmailObligation(actionName, startTime, newAttList);
		}
		return obl;
	}

	/**
	 * this timer to check whether the obligation is fulfilled or not
	 * @author anitacao
	 *
	 */
	public class OblCheckerTimerTask extends TimerTask {
		UUID uuid;
		Obligation obl;
		OpenmrsUserObligationMonitor monitor;
		String userId;
		public OblCheckerTimerTask(UUID uuid,OpenmrsUserObligationMonitor monitor) {
			this.uuid = uuid;
			this.monitor = monitor;
		}
		
		public void run() {
			if(activeObs.containsKey(uuid)){
				UserObRelation uo  = activeObs.get(uuid);
				String userId = uo.userId;
				String oblId = uo.obId;
				Date startDate = uo.date;
			
				obl = getObfromDB(oblId,startDate);
				
				if(obl.isSatisfied(userId,uuid.toString())){
					
					ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
					query.add(new AttributeQuery(SubjectAttributeXmlName.ID, userId,StringAttribute.identifier));
					List<String> budgets = dh.getAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query, "budget");
					String currentBudget = budgets.get(0);
					
					String decreasedBudget = uo.decreasedBudget;
					
					String newBudget = String
							.valueOf(Double.parseDouble(currentBudget) + Double.parseDouble(decreasedBudget));
					
					query.clear();
					query.add(new AttributeQuery(SubjectAttributeXmlName.BUDGET, newBudget,
							StringAttribute.identifier));
					dh.modifyAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, userId, query);
					
					System.err.println("Anita, the new budget is : "+getObfromDB(oblId,startDate).getAttribute("budget"));
					
					//remove from the active list, add to the fulfilled list in openmrs enfroce service context. 
					activeObs.remove(uuid);
					fulfilledObs.put(uuid, uo);
					OpenmrsEnforceServiceContext.setActiveOb(activeObs);
					OpenmrsEnforceServiceContext.setFulfilledOb(fulfilledObs);
					System.err.println("Anita, now the active obligation is : " + activeObs.size());
					System.err.println("Anita, now the fulfilled obligation is : " + fulfilledObs.size());
					monitorableObject.notifyFulfillment(obl);
				}
			}
		}
	}
	
	public class OblDeadlineTimerTask extends TimerTask {
		UUID uuid;
		Obligation obl;
		OpenmrsUserObligationMonitor monitor;
		public OblDeadlineTimerTask(UUID uuid,OpenmrsUserObligationMonitor monitor) {
			this.uuid = uuid;
			this.monitor = monitor;
		
		}
		
		public void run() {
			
			if(activeObs.containsKey(uuid)){
				UserObRelation uo  = activeObs.get(uuid);
				String oblId = uo.obId;
				Date startDate = uo.date;
				obl = getObfromDB(oblId,startDate);
				
				//remove from the active list, add to the expired list in openmrs enfroce service context. 
				activeObs.remove(uuid);
				expiredObs.put(uuid, uo);
				OpenmrsEnforceServiceContext.setActiveOb(activeObs);
				OpenmrsEnforceServiceContext.setExpiredOb(expiredObs);
				
				monitorableObject.notifyDeadline(obl);
			}
		}
	}
	}		
	

	
	

