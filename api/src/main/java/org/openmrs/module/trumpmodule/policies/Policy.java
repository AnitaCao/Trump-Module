package org.openmrs.module.trumpmodule.policies;

import java.io.Serializable;
import org.openmrs.BaseOpenmrsData;
public class Policy extends BaseOpenmrsData implements Serializable {
	

	private static final long serialVersionUID = 1L;
	private Integer id;
	private String content;
	private String policyName;
	private String userId; //belong to which user(should the user be patient ?)
	
	public Policy(){
		//this.userId = userId;
		// TODO - fix this - super dirty generation of a not very unique integer ID just to keep 
		// OpenMRS happy for now. But we will need a better solution in production. It seems that
		// openmrs objects get their integer IDs after serialisation in the database (a bit like
		// Ruby on Rails ) and since we are not using the database, we don't get this. But we are
		// only going to use UUID anyway so it shouldn't be a problem.
		this.id = new Long(System.currentTimeMillis() / 1000L).intValue();
	}
	
	public Policy(String name, String uid, String content) {
		this.policyName = name;
	    this.userId = uid;
	    this.content = content;
		this.id = new Long(System.currentTimeMillis() / 1000L).intValue();

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
		// TODO the content should be returned as an XML string and not just a normal string, because
		// we end up sending a lot of newline and tab characters unneccessarily.
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
