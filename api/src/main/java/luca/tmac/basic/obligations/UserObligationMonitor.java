package luca.tmac.basic.obligations;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import org.wso2.balana.ParsingException;
import org.wso2.balana.attr.DateTimeAttribute;
import org.wso2.balana.attr.StringAttribute;

public class UserObligationMonitor {

	Timer timer;
	List<Obligation> obligationList;
	List<ObligationSet> oblSetList;
	ObligationMonitorable monitorableObject;
	DataHandler dh = null;

	public List<Obligation> getList() {
		return obligationList;
	}

	public UserObligationMonitor(List<Obligation> oblList,
			ObligationMonitorable pm, DataHandler handler) {
		monitorableObject = pm;
		timer = new Timer();
		obligationList = new ArrayList<Obligation>();
		oblSetList = new ArrayList<ObligationSet>();
		dh = handler;
	}

	public UserObligationMonitor(ObligationMonitorable pm, DataHandler handler) {
		this(new ArrayList<Obligation>(), pm, handler);
	}

	public void addObligation(Obligation obl) {
		loadObligation(obl);
		writeToDb(obl);
	}

	
	public void loadObligation(Obligation obl) {
		if (obl.isActive()) {
			obligationList.add(obl);
			Date deadline = obl.getDeadline();
			timer.schedule(new myTimerTask(obl, this), deadline);
			monitorableObject.notifyObligationInsert(obl);
		}
	}

	
	public void loadListFromDb() {
		List<Obligation> list = getListFromDb(); //get obligations from data.xml file. 
		obligationList.clear();
		timer.cancel();
		timer = new Timer();
		for (Obligation obl : list)
			this.loadObligation(obl);
	}

	private List<Obligation> getListFromDb() {
		ArrayList<Obligation> oblList = new ArrayList<Obligation>();
		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		List<String> ids = dh.getAttribute("obligation", query, "id");

		for (String oblId : ids) {

			// in the db we don't need to write the deadline value because we can compute it with the start_time 
			//and the duration, both of those are saved in the db.

			List<AttributeQuery> attributes = dh.getAttributesOf("obligation",
					oblId);
			ArrayList<AttributeQuery> newAttList = new ArrayList<AttributeQuery>();
			String actionName = null;
			Date startTime = null;
			for (AttributeQuery att : attributes) {
				if (att.name
						.equals(ObligationIds.ACTION_NAME_OBLIGATION_ATTRIBUTE)) {
					actionName = att.value;
				} else if (att.name
						.equals(ObligationIds.START_TIME_OBLIGATION_ATTRIBUTE)) {
					try {
						startTime = DateTimeAttribute.getInstance(att.value)
								.getValue();
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (ParsingException e) {
						e.printStackTrace();
					}
				} else {
					newAttList.add(att);
				}
			}
			
		}
		return oblList;
	}

	public void writeToDb(Obligation obl) {
		ArrayList<AttributeQuery> aq = new ArrayList<AttributeQuery>();
		aq.add(new AttributeQuery(
				ObligationIds.ACTION_NAME_OBLIGATION_ATTRIBUTE, obl.getActionName(),
				StringAttribute.identifier));
		aq.add(new AttributeQuery(
				ObligationIds.START_TIME_OBLIGATION_ATTRIBUTE,
				new DateTimeAttribute(obl.getStartDate()).encode(),
				DateTimeAttribute.identifier));

		for (String parName : obl.getAttributeMap().keySet()) {
			aq.add(new AttributeQuery(parName,obl.getAttributeMap().get(parName),StringAttribute.identifier));
		}
		String id = dh.write("obligation", aq); //write the obligation to database 
		obl.setAttribute(new AttributeQuery("id", id,
				StringAttribute.identifier));
	}

	public class myTimerTask extends TimerTask {
		Obligation obl;
		UserObligationMonitor monitor;

		public myTimerTask(Obligation pObl, UserObligationMonitor pMonitor) {
			obl = pObl;
			monitor = pMonitor;
		}

		public void run() {
			if(!obl.isActive())
			{
				return;
			}
			
			monitor.obligationList.remove(obl);
			obl.setAttribute(new AttributeQuery(Obligation.STATE_ATTRIBUTE_NAME,Obligation.STATE_EXPIRED,StringAttribute.identifier)); //put the value : expired , the name: state to a hashmap 
			dh.remove("obligation", obl.getAttribute("id"));
			writeToDb(obl);
			if (monitor.monitorableObject != null && !obl.isFulfilled())
				monitor.monitorableObject.notifyDeadline(obl);
			// TODO implement timer Expiration task...something else to do?
		}
	}
	
	public void obligationFulfilled(Obligation obl)
	{
		if(obl.isActive() && obl.getDeadline().after(new Date()))
		{
			this.obligationList.remove(obl);
			AttributeQuery aq = new AttributeQuery(Obligation.STATE_ATTRIBUTE_NAME,Obligation.STATE_FULFILLED,StringAttribute.identifier);
			obl.setAttribute(aq);
			dh.remove("obligation", obl.getAttribute("id"));
			writeToDb(obl);
			monitorableObject.notifyFulfillment(obl);
		}
	}
}
