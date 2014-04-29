package org.openmrs.module.trumpmodule.obligations;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.openmrs.BaseOpenmrsData;

import luca.data.AttributeQuery;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationImpl;

public class RESTObligation extends BaseOpenmrsData implements Serializable, Obligation {
	private Integer id;
	private ObligationImpl ob;
	
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

	public String getDecreasedBudget() {
		// TODO Auto-generated method stub
		return ob.getDecreasedBudget();
	}

	public void setDecreasedBudget(String decreasedBudget) {
		// TODO Auto-generated method stub
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

}
