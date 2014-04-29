package luca.tmac.basic.obligations;

import java.util.Date;
import java.util.HashMap;

import luca.data.AttributeQuery;

public interface Obligation {

	public static final String STATE_FULFILLED = "fulfilled";
	public static final String STATE_ACTIVE = "active";
	public static final String STATE_EXPIRED = "expired";
	public static final String STATE_ATTRIBUTE_NAME = "state";
	public static final String SET_ID_ATTRIBUTE_NAME = "setId";


	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#getAttribute(java.lang.String)
	 */
	public abstract String getAttribute(String name);

	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#getDeadline()
	 */
	public abstract Date getDeadline();

	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#toString()
	 */
	public abstract String toString();

	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isUserObligation()
	 */
	public abstract boolean isUserObligation();

	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isSystemObligation()
	 */
	public abstract boolean isSystemObligation();
	


	public abstract boolean isSatisfied(String userId, String uuid);

	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#setAttribute(luca.data.AttributeQuery)
	 */
	public abstract void setAttribute(AttributeQuery aq);

	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isFulfilled()
	 */
	public abstract Boolean isFulfilled();
	
	public abstract void setFulfilled(boolean fulfilled);

	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isActive()
	 */
	public abstract Boolean isActive();

	public abstract void setActive(Boolean active);
	
	/* (non-Javadoc)
	 * @see luca.tmac.basic.obligations.Obligation#isExpired()
	 */
	public abstract Boolean isExpired();
	
	public abstract void setExpired(Boolean expired);
	
	public String getActionName();

	public void setActionName(String actionName);

	public HashMap<String, String> getAttributeMap();

	public void setAttributeMap(HashMap<String, String> attributeMap);

	public Date getStartDate();

	public void setStartDate(Date startDate);
	
	public String getObUUID();
	
	public void setObUUID(String obUUID);

	public String getUserId();

	public void setUserId(String userId);
	
	public String getDecreasedBudget();

	public void setDecreasedBudget(String decreasedBudget);

}