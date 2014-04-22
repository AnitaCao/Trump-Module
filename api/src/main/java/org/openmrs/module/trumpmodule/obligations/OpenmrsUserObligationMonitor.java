package org.openmrs.module.trumpmodule.obligations;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.wso2.balana.ParsingException;
import org.wso2.balana.attr.DateTimeAttribute;
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
	private ArrayList<Obligation> activeObs;
	private ArrayList<Obligation> fulfilledObs;
	private ArrayList<Obligation> expiredObs;
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
		
//		if(!activeObs.isEmpty()){
//			checkObligations();
//		}
	}
	
	public void checkObligations() {
		if(!activeObs.isEmpty()){
			for(Obligation ob : activeObs){
				checkObligation(ob);
			}
//			for(UUID uuid : activeObs.keySet()){
//				checkObligation(uuid);
//			}
		}
	}
	
//	public void checkObligation(UUID uuid) {
//		//UserObRelation uo = activeObs.get(uuid);
//		String actionName = uo.actionName;
//		Date startDate = uo.startDate;
//		Obligation obl = getObfromDB(actionName,startDate);
//	
//		Date deadline = obl.getDeadline();
//		System.err.println("Anita ! the deadline of this obligation is : " + deadline);
//		timer.schedule(new OblDeadlineTimerTask(uuid, this), deadline);
//			
//		timer2.schedule(new OblCheckerTimerTask(uuid, this) , 0 , 60000);
//		
//		
//	}
	public void checkObligation(Obligation ob) {
	//UserObRelation uo = activeObs.get(uuid);
//	String actionName = ob.getActionName();
//	Date startDate = ob.getStartDate();
	//Obligation obl = getObfromDB(actionName,startDate);

	Date deadline = ob.getDeadline();
	System.err.println("Anita ! the deadline of this obligation is : " + deadline);
	timer.schedule(new OblDeadlineTimerTask(ob, this), deadline);
		
	timer2.schedule(new OblCheckerTimerTask(ob, this) , 0 , 60000);
	
	
}
	
	/**
	 * get obligation from database according to the obligation id. 
	 * @param oblId
	 * @return obl Obligation 
	 */
//	public Obligation getObfromDB(String oblId,Date startTime){
//		Obligation obl = null;
//		List<AttributeQuery> attributes = dh.getAttributesOf("obligation",
//				oblId);
//		ArrayList<AttributeQuery> newAttList = new ArrayList<AttributeQuery>();
//		String actionName = null;
//		for (AttributeQuery att : attributes) {
//			if (att.name
//					.equals(ObligationIds.ACTION_NAME_OBLIGATION_ATTRIBUTE)) {
//				actionName = att.value;
//			} else {
//				newAttList.add(att);
//			}
//		}
//		if(actionName.equals(ObligationIds.EMAIL_OBLIGATION_NAME_XML)){
//			obl = new EmailObligation(actionName, startTime, newAttList);
//		} else if(actionName.equals(ObligationIds.REST_OBLIGATION_NAME_XML)){
//			obl = new RESTObligation(actionName, startTime, newAttList);
//
//		}
//		return obl;
//	}

	
	/**
	 * this timer to check whether the obligation is fulfilled or not
	 * @author anitacao
	 *
	 */
	public class OblCheckerTimerTask extends TimerTask {
		UUID uuid;
		Obligation ob;
		OpenmrsUserObligationMonitor monitor;
		String userId;
		public OblCheckerTimerTask(Obligation ob,OpenmrsUserObligationMonitor monitor) {
			this.ob = ob;
			this.monitor = monitor;
		}
		  
		public void run() {
			if(activeObs.contains(ob)){
				String userId = ob.getUserId();
//				String actionName = ob.getActionName();
//				Date startDate = ob.getStartDate();
				uuid = ob.getObUUID();
			
				//ob = getObfromDB(actionName,startDate);
				
				if(ob.isSatisfied(userId,uuid.toString())){
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
					fulfilledObs.add(ob);
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
		UUID uuid;
		Obligation ob;
		OpenmrsUserObligationMonitor monitor;
//		public OblDeadlineTimerTask(UUID uuid,OpenmrsUserObligationMonitor monitor) {
//			this.uuid = uuid;
//			this.monitor = monitor;
//		
//		}
		public OblDeadlineTimerTask(Obligation ob, OpenmrsUserObligationMonitor monitor){
			this.ob = ob;
			this.monitor = monitor;
		}
		
		public void run() {
			if(activeObs.contains(ob)){
			//if(activeObs.containsKey(uuid)){
				timer.cancel();
				//UserObRelation uo  = activeObs.get(uuid);
//				String actionName = ob.getActionName();
//				Date startDate = ob.getStartDate();
				//ob = getObfromDB(actionName,startDate);
				
				//remove from the active list, add to the expired list in openmrs enfroce service context. 
				activeObs.remove(ob);
				expiredObs.add(ob);
				SerContext.setActiveObs(activeObs);
				SerContext.setExpiredObs(expiredObs);
				monitorableObject.notifyDeadline(ob);
			}
		}
	}
	
//	protected List<Obligation> getListFromDb() {
//		ArrayList<Obligation> oblList = new ArrayList<Obligation>();
//		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
//		List<String> ids = dh.getAttribute("obligation", query, "id");
//
//		for (String oblId : ids) {
//
//			// in the db i don't need to write the deadline value cause i can
//			// compute it with the start_time and the duration
//			// both of those are saved in the db
//
//			List<AttributeQuery> attributes = dh.getAttributesOf("obligation",
//					oblId);
//			ArrayList<AttributeQuery> newAttList = new ArrayList<AttributeQuery>();
//			String actionName = null;
//			Date startTime = null;
//			for (AttributeQuery att : attributes) {
//				if (att.name
//						.equals(ObligationIds.ACTION_NAME_OBLIGATION_ATTRIBUTE)) {
//					actionName = att.value;
//				} else if (att.name
//						.equals(ObligationIds.START_TIME_OBLIGATION_ATTRIBUTE)) {
//					try {
//						startTime = DateTimeAttribute.getInstance(att.value)
//								.getValue();
//					} catch (ParseException e) {
//						e.printStackTrace();
//					} catch (NumberFormatException e) {
//						e.printStackTrace();
//					} catch (ParsingException e) {
//						e.printStackTrace();
//					}
//				} else {
//					newAttList.add(att);
//				}
//			}
//			
//			// TODO: Chris: here we need to check what kind of obligation this is,
//			// probably by using the XML name, and create the appropriate subclass. The
//			// ArrayList will take anything that is an Obligation, but we need to call
//			// the correct constructor so need the exact type here.
//			Obligation ob = null;
//			
//			if(actionName.equals(ObligationIds.EMAIL_OBLIGATION_NAME_XML)){
//				ob = new EmailObligation(actionName, startTime, newAttList);
//			}else if(actionName.equals(ObligationIds.REST_OBLIGATION_NAME_XML)){
//				ob = new RESTObligation(actionName, startTime, newAttList);
//				System.err.println("Anita !!!! the obligation name is : " + actionName );
//			}
//			oblList.add(ob);
//		}
//		return oblList;
//	}
	
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
	

	
	

