package org.openmrs.module.trumpmodule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.data.XmlDataHandler;
import luca.tmac.basic.data.xml.SubjectAttributeXmlName;
import luca.tmac.basic.obligations.Obligation;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.policies.Policy;
import org.openmrs.module.trumpmodule.policies.PolicyFileHandler;
import org.wso2.balana.attr.StringAttribute;

/**
 * this singleton class contains : .....
 * @author anitacao
 *
 */
public class OpenmrsEnforceServiceContext {

	
	//userOb stores the relationship between user and user obligations, which means it stores which user have which 
	//obligations,the key is an UUID , the value is a UserObRelation object, which contains userId, actionName, start 
	//date and decreasedBudget related to this obligation
//	private HashMap<UUID, Obligation> activeObs = null;
//
//
//	//fulfilledOb stores fulfilled obligation, the key is the uuid , the value is a UserObRelation object
//	private HashMap<UUID, Obligation> fulfilledObs = null;
//	
//	//fulfilledOb stores expiredOb obligation, the key is the uuid , the value is a UserObRelation object
//	private HashMap<UUID, Obligation> expiredObs = null;
	
	private ArrayList<Obligation> activeObs;
	

	private ArrayList<Obligation> fulfilledObs;
	private ArrayList<Obligation> expiredObs;
	
	//assignedPatientInternalIds stores the assigned patients' ids of the user (doctor), the key is the userId, the value is a set which contians the assigned patients' patient_id
	private HashMap<String, HashSet<String>> AssigendPatientInternalIds = null;
	
	
	//each user have multiple policies
	private HashMap<String, ArrayList<Policy>> policies = null;
	
	private DataHandler dh = null; 
	
	private static OpenmrsEnforceServiceContext enforceSerContext = new OpenmrsEnforceServiceContext();
	
	private static final String RESOURCE_PATH = "data.xml";


	private OpenmrsEnforceServiceContext(){
		activeObs = new ArrayList<Obligation>();
		fulfilledObs = new ArrayList<Obligation>();
		expiredObs = new ArrayList<Obligation>();
		AssigendPatientInternalIds = new HashMap<String, HashSet<String>>();
		policies = new HashMap<String, ArrayList<Policy>>();
		
		String path = this.getClass().getClassLoader().getResource(RESOURCE_PATH).toString().substring(5);
		dh = new XmlDataHandler(path);
		
		initializedAssignedPatient();
		// load the policies into context hash from file system
		try {
			policies = PolicyFileHandler.loadPolicies(this.getUserPolicyDirectory());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public HashMap<String, ArrayList<Policy>> getPolicies() {
		return policies;
	}
	
	// return the directory containing top-level policies 
	public String getTopLevelPolicyDirectory() {
		String path = System.getProperty("user.dir");
		return path + File.separator + "top_level_policies";
	}
	
	// return the directory containing user policies
	public String getUserPolicyDirectory() {
		String path = System.getProperty("user.dir");
		return path + File.separator + "user_policies";
	}

	public void setPolicies(HashMap<String, ArrayList<Policy>> policies) {
		this.policies = policies;
	}

	/**
	 * In this method, we resolve the ids of users and assigned patients specified in the data.xml file, to 
	 * openmrs "internal" database identifiers
	 */
	public void initializedAssignedPatient(){
		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		List<String> userIds = dh.getAttribute("user", query, "id");
		for(String id : userIds){
			ArrayList<AttributeQuery> query2 = new ArrayList<AttributeQuery>();
			query2.add(new AttributeQuery(SubjectAttributeXmlName.ID, id ,StringAttribute.identifier));
			
		
			List<String> stringAttributes = dh.getAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query2,
					SubjectAttributeXmlName.ASSIGNED_PATIENT);
			
			HashSet<String> set = new HashSet<String>();
			
			for(String patientidentifierId : stringAttributes){
				
				List<Patient> patients = Context.getPatientService().getPatients(null, patientidentifierId, null,false);
				
				for(Patient p : patients){
					set.add(p.getPatientId().toString());
				}
			}
			
			AssigendPatientInternalIds.put(id, set);
		}
	}
	
	
	public DataHandler getDh() {
		return dh;
	}

	public void setDh(DataHandler dh) {
		this.dh = dh;
	}

	public void initialAssiPatiInteIds (){
		
	}

	public static OpenmrsEnforceServiceContext getInstance(){
		return enforceSerContext;
	}

	public HashMap<String, HashSet<String>> getAssigendPatientInternalIds() {
		
		return AssigendPatientInternalIds;
	}
	
	


	public void setAssigendPatientInternalIds(
			HashMap<String, HashSet<String>> assigendPatientInternalIds) {
		AssigendPatientInternalIds = assigendPatientInternalIds;
	}


	public ArrayList<Policy> getPolicyByPerson(String personId) {
		ArrayList<Policy> policy = null;
		if(policies.containsKey(personId)){
			policy = policies.get(personId);
		}
		return policy;
		
	}
	
	public List<Policy> getAllPolicies(){
		
		List<Policy> allPolicies = new ArrayList<Policy>();
		for(Entry<String,ArrayList<Policy>> e : policies.entrySet()) {
			allPolicies.addAll(e.getValue());
		}
		return allPolicies;
	}
	
	public Policy getPolicyByPolicyID(String policyId) {
		Policy policy = null;
		for(Entry<String,ArrayList<Policy>> e : policies.entrySet()) {
			for(Policy p : e.getValue()){
				if(p.getUuid().equals(policyId)){
					policy = p;
					break;
				}
			}
		}
		return policy;
	}

	public ArrayList<Obligation> getActiveObs() {
		return activeObs;
	}

	public void setActiveObs(ArrayList<Obligation> activeObs) {
		this.activeObs = activeObs;
	}

	public ArrayList<Obligation> getFulfilledObs() {
		return fulfilledObs;
	}

	public void setFulfilledObs(ArrayList<Obligation> fulfilledObs) {
		this.fulfilledObs = fulfilledObs;
	}

	public ArrayList<Obligation> getExpiredObs() {
		return expiredObs;
	}

	public void setExpiredObs(ArrayList<Obligation> expiredObs) {
		this.expiredObs = expiredObs;
	}
}
