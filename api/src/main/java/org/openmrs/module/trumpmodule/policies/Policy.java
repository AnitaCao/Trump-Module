package org.openmrs.module.trumpmodule.policies;

import java.io.Serializable;
import java.util.UUID;

import org.openmrs.BaseOpenmrsData;
public class Policy extends BaseOpenmrsData implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	UUID uuid = UUID.randomUUID();
	private Integer id;
	private String content;
	private String policyName;
	private String userId; //belong to which user(should the user be patient ?)
	
	public Policy(){
		//this.userId = userId;
		this.id = (int) uuid.getMostSignificantBits();
	}
	
	public Policy(String name, String uid, String content) {
		this.policyName = name;
		this.id = (int) uuid.getMostSignificantBits();
	    this.userId = uid;
	    this.content = content;
	}
	
	public Policy(String path){
		
	}
	
	public Policy(String personId, String content){
		
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getPolicyName() {
		return policyName;
	}

	public void setPolicyName(String policyName) {
		this.policyName = policyName;
	}

	public Integer getId() {
		// this is bad - but to be an OpenMRSData it needs to
		return (int) this.id;
	}

	public void setId(Integer newId) {
		this.id = newId;
		
	}
	
	


}
