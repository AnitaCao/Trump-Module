package org.openmrs.module.trumpmodule;

import java.util.HashMap;
import java.util.UUID;

import org.openmrs.module.trumpmodule.obligations.UserObRelation;

/**
 * this singleton class contains : .....
 * @author anitacao
 *
 */
public class OpenmrsEnforceServiceContext {
	
	//userOb stores the relationship between user and user obligations, which means it stores which user have which 
	//obligations,the key is an UUID , the value is a string array with length of 3,which stores the userId and obligationId 
	//and the start time. 
	private static HashMap<UUID, UserObRelation> activeObs = new HashMap<UUID,UserObRelation>();

	//fulfilledOb stores fulfilled obligation, the key is the uuid , the value is obligationId
	private static HashMap<UUID, UserObRelation> fulfilledObs = new HashMap<UUID,UserObRelation>();
	
	//fulfilledOb stores expiredOb obligation, the key is the uuid , the value is obligationId
	private static HashMap<UUID, UserObRelation> expiredObs = new HashMap<UUID,UserObRelation>();
	
	private static OpenmrsEnforceServiceContext enforceSerContext = new OpenmrsEnforceServiceContext();

	private OpenmrsEnforceServiceContext(){
		activeObs = new HashMap<UUID, UserObRelation>();
	}
	
	public static OpenmrsEnforceServiceContext getInstance(){
		return enforceSerContext;
	}
	
	public static HashMap<UUID, UserObRelation> getActiveOb() {
		return activeObs;
	}

	public static void setActiveOb(HashMap<UUID, UserObRelation> activeOb) {
		OpenmrsEnforceServiceContext.activeObs = activeOb;
	}

	public static HashMap<UUID, UserObRelation> getFulfilledOb() {
		return fulfilledObs;
	}

	public static void setFulfilledOb(HashMap<UUID, UserObRelation> fulfilledOb) {
		OpenmrsEnforceServiceContext.fulfilledObs = fulfilledOb;
	}

	public static HashMap<UUID, UserObRelation> getExpiredOb() {
		return expiredObs;
	}

	public static void setExpiredOb(HashMap<UUID, UserObRelation> expiredOb) {
		OpenmrsEnforceServiceContext.expiredObs = expiredOb;
	}
	
}
