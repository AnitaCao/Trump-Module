package org.openmrs.module.trumpmodule.obligations;

import java.util.Date;

public class UserObRelation {
	public String userId;
	public String obId;
	public Date date;
	public String decreasedBudget;
	
	public UserObRelation(String userId,String obId, Date date, String decreasedBudget)
	{
		this.userId = userId;
		this.obId = obId;
		this.date = date;
		this.decreasedBudget = decreasedBudget;
	}
}
