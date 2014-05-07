package org.openmrs.module.trumpmodule.obligations;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;

import luca.data.AttributeQuery;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationImpl;

public class RESTObligation extends BaseOpenmrsData implements Serializable, Obligation {
	private Integer id;
	private ObligationImpl ob;
	private OpenmrsUserObligationMonitor openmrsOblMonitor = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public RESTObligation(String actionName,String userId, Date pStartDate, List<AttributeQuery> pParameters){
		// our 'wrapped' obligation instance
		ob = new ObligationImpl(actionName, userId, pStartDate, pParameters);
		id = (int)UUID.randomUUID().getMostSignificantBits();
	}
	
	/**
	 * Default constructor required by REST framework
	 */
	public RESTObligation() {
		
	}
	
	/**
	 * Create a new RESTObligation from an abstract non-rest one, RESTifying it.
	 * @param ob
	 */
	public RESTObligation(ObligationImpl ob) {
		this.ob = ob;
		id = (int)UUID.randomUUID().getMostSignificantBits();
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAttribute(String name) {
		return ob.getAttribute(name);
	}

	public Date getDeadline() {
		return ob.getDeadline();
	}

	public boolean isUserObligation() {
		return ob.isUserObligation();
	}

	public boolean isSystemObligation() {
		return ob.isSystemObligation();
	}

	public boolean isSatisfied(String userId, String uuid) {
		return ob.isSatisfied(userId, uuid);
	}

	public void setAttribute(AttributeQuery aq) {
		ob.setAttribute(aq);
	}

	public Boolean isFulfilled() {
		return ob.isFulfilled();
	}
	
	public Boolean getFulfilled() {
		return ob.isFulfilled();
	}
	
	public void setFulfilled(Boolean fulfilled) {
		ob.setFulfilled(fulfilled);
		openmrsOblMonitor.updateBudget(ob,ob.getUserId());
		System.err.println("Anita, the size of fulfilledObs is : "+ OpenmrsEnforceServiceContext.getInstance().getFulfilledObs().size());
		System.err.println("Anita, the size of userObs is : "+ OpenmrsEnforceServiceContext.getInstance().getUserObs().size());
		System.err.println("Anita, the size of roleObs is : "+ OpenmrsEnforceServiceContext.getInstance().getRoleObs().size());
		System.err.println("Anita, the size of oblsSets is : "+ OpenmrsEnforceServiceContext.getInstance().getObligationSets().size());
	}


	public Boolean isActive() {
		return ob.isActive();
	}
	
	public Boolean getActive() {
		return ob.isActive();
	}
	
	public void setActive(Boolean active) {
		ob.setActive(active);
	}

	public Boolean isExpired() {
		return ob.isExpired();
	}
	
	public Boolean getExpired() {
		return ob.isExpired();
	}
	
	public void setExpired(Boolean expired) {
		ob.setExpired(expired);
	}

	public String getActionName() {
		return ob.getActionName();
	}

	public void setActionName(String actionName) {
		ob.setActionName(actionName);
	}

	public HashMap<String, String> getAttributeMap() {
		return ob.getAttributeMap();
	}

	public void setAttributeMap(HashMap<String, String> attributeMap) {
		ob.setAttributeMap(attributeMap);
	}

	public Date getStartDate() {
		return ob.getStartDate();
	}

	public void setStartDate(Date startDate) {
		ob.setStartDate(startDate);
	}

	public String getObUUID() {
		return this.getUuid();
	}

	public void setObUUID(String obUUID) {
		ob.setObUUID(obUUID);
		
	}

	public String getUserId() {
		return ob.getUserId();
	}

	public void setUserId(String userId) {
		ob.setUserId(userId);
	}
	
	public String getTriggeringUserId(){
		return ob.getTriggeringUserId();
	}
	
	public void setTriggeringUserId(String triggeringUserId){
		ob.setTriggeringUserId(triggeringUserId);
	}

	public String getDecreasedBudget() {
		return ob.getDecreasedBudget();
	}

	public void setDecreasedBudget(String decreasedBudget) {
		ob.setDecreasedBudget(decreasedBudget);
	}

	/**
	 * Display string for this object
	 * @return a string summarising this object
	 */
	public String getObligationDisplayString() {
		return getUserId() + ":" + getActionName();
	}

	public void setActive(boolean active) {
		ob.setActive(active);
		
	}

	public void setExpired(boolean expired) {
		ob.setExpired(expired);
	}

	@Override
	public String getSetId() {
		return ob.getSetId();
	}

	@Override
	public void setSetId(String setId) {
		ob.setSetId(setId);
	}
	
	public OpenmrsUserObligationMonitor getOpenmrsOblMonitor() {
		return openmrsOblMonitor;
	}

	public void setOpenmrsOblMonitor(OpenmrsUserObligationMonitor openmrsOblMonitor) {
		this.openmrsOblMonitor = openmrsOblMonitor;
	}
}
