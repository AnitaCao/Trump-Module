package luca.tmac.basic.obligations;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.obligations.OpenmrsUserObligationMonitor;
import org.wso2.balana.ParsingException;
import org.wso2.balana.attr.DayTimeDurationAttribute;
import org.wso2.balana.attr.StringAttribute;

import luca.data.AttributeQuery;

public class ObligationImpl implements Obligation {
	
	private String actionName;
	private HashMap<String,String> attributeMap;
	private Date startDate;
	private String obUUID;
	private String userId;
	private String triggeringUserId;
	private String decreasedBudget;
	private String setId;
	
	
	
	public ObligationImpl(){
		
	}

	public ObligationImpl(String actionName,String triggeringUserId, Date pStartDate, List<AttributeQuery> parameters) {
		this.obUUID = UUID.randomUUID().toString();
		this.actionName = actionName;
		this.startDate = pStartDate;
		this.triggeringUserId = triggeringUserId;
		
		this.attributeMap = new HashMap<String,String>();
		for(AttributeQuery aq : parameters)
		{
			attributeMap.put(aq.name, aq.value);
			
		}
		attributeMap.put(STATE_ATTRIBUTE_NAME, STATE_ACTIVE);
	}
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#getAttribute(java.lang.String)
	 */
	@Override
	public String getAttribute(String name)
	{
		if(name.equals(ObligationIds.ACTION_NAME_OBLIGATION_ATTRIBUTE))
			return actionName;
		return attributeMap.get(name);
	}
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#getDeadline()
	 */
	@Override
	public Date getDeadline()
	{
		String xmlDurationString = this
				.getAttribute(ObligationIds.DURATION_OBLIGATION_ATTRIBUTE);
		long duration = 0;
		try {
			duration = DayTimeDurationAttribute.getInstance(xmlDurationString)
					.getTotalSeconds();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ParsingException e) {
			e.printStackTrace();
		}
		
		Date deadline = new Date(startDate.getTime() + duration);
		return deadline;
	}
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#toString()
	 */
	@Override
	public String toString() {
		String out = "Obl[Id = " + actionName;
		for ( String aName : attributeMap.keySet()) {
			out += ", " + aName + " = " + attributeMap.get(aName);
		}
		out += "]";
		return out;
	}

	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isUserObligation()
	 */
	@Override
	public boolean isUserObligation()
	{
		return actionName.startsWith(ObligationIds.USER_PREFIX);
	}
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isSystemObligation()
	 */
	@Override
	public boolean isSystemObligation()
	{
		return !isUserObligation();
	}
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isSatisfied(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isSatisfied(String userId,String uuid){
		return false;
	}
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#setAttribute(luca.data.AttributeQuery)
	 */
	@Override
	public void setAttribute(AttributeQuery aq)
	{
		attributeMap.put(aq.name, aq.value);
	}
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isFulfilled()
	 */
	@Override
	public Boolean isFulfilled()
	{
		String stateAttrName =  getAttribute(STATE_ATTRIBUTE_NAME);
		return stateAttrName != null && stateAttrName.equals(STATE_FULFILLED);
	}
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isActive()
	 */
	@Override
	public Boolean isActive()
	{
		String state =  getAttribute(STATE_ATTRIBUTE_NAME);
		return state == null || state.equals(STATE_ACTIVE);
		
	}
	
 	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isExpired()
	 */
 	@Override
	public Boolean isExpired()
	{
		String state =  getAttribute(STATE_ATTRIBUTE_NAME);
		return state != null && state.equals(STATE_EXPIRED);	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public HashMap<String, String> getAttributeMap() {
		return attributeMap;
	}

	public void setAttributeMap(HashMap<String, String> attributeMap) {
		this.attributeMap = attributeMap;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	public String getObUUID() {
		return obUUID;
	}

	public void setObUUID(String obUUID) {
		this.obUUID = obUUID;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getTriggeringUserId() {
		return triggeringUserId;
	}

	public void setTriggeringUserId(String triggeringUserId) {
		this.triggeringUserId = triggeringUserId;
	}

	public String getDecreasedBudget() {
		return decreasedBudget;
	}

	public void setDecreasedBudget(String decreasedBudget) {
		this.decreasedBudget = decreasedBudget;
	}

	public String getSetId() {
		return setId;
	}

	public void setSetId(String setId) {
		this.setId = setId;
	}

	@Override
	public void setFulfilled(Boolean fulfilled) {
		if(fulfilled) {
			setAttribute(new AttributeQuery(STATE_ATTRIBUTE_NAME, STATE_FULFILLED, StringAttribute.identifier));
			OpenmrsEnforceServiceContext.getInstance().getActiveObs().remove(this);
			OpenmrsEnforceServiceContext.getInstance().getFulfilledObs().put(obUUID, this);
			OpenmrsEnforceServiceContext.getInstance().getObligationSets().get(setId).remove(this);
			
			if(OpenmrsEnforceServiceContext.getInstance().getObligationSets().get(setId).isEmpty()){
				OpenmrsEnforceServiceContext.getInstance().getObligationSets().remove(setId);
			}
		}
		else {
			setAttribute(new AttributeQuery(STATE_ATTRIBUTE_NAME, STATE_ACTIVE, StringAttribute.identifier));
			OpenmrsEnforceServiceContext.getInstance().getFulfilledObs().remove(this);
			OpenmrsEnforceServiceContext.getInstance().getActiveObs().put(obUUID, this);
		}
	}



	@Override
	public void setActive(Boolean active) {
		if(active)
			setAttribute(new AttributeQuery(STATE_ATTRIBUTE_NAME, STATE_ACTIVE, StringAttribute.identifier));

		else
			setAttribute(new AttributeQuery(STATE_ATTRIBUTE_NAME, STATE_EXPIRED, StringAttribute.identifier));
		
	}

	@Override
	public void setExpired(Boolean expired) {
		if(expired)
			setAttribute(new AttributeQuery(STATE_ATTRIBUTE_NAME, STATE_EXPIRED, StringAttribute.identifier));
		else
			setAttribute(new AttributeQuery(STATE_ATTRIBUTE_NAME, STATE_ACTIVE, StringAttribute.identifier));
		
	}



}
