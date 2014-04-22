package org.openmrs.module.trumpmodule;

import java.util.Date;

import luca.tmac.basic.obligations.Obligation;

public class ObBudgetRelation {
	//public String userId;
	//public String obId;
	public Obligation obligation;
	//public Date startDate;
	public String decreasedBudget;
	
	public ObBudgetRelation(String userId,Obligation ob, Date date, String decreasedBudget)
	{
		//this.userId = userId;
		//this.obId = obId;
		this.obligation = ob;
		//this.startDate = date;
		this.decreasedBudget = decreasedBudget;
	}
}
