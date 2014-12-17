package org.openmrs.module.trumpmodule.web.resource;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import luca.tmac.basic.data.uris.ProvenanceStrings;

import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.aop.AuthorizationAdvice;
import org.openmrs.module.trumpmodule.aop.ProvenanceAdvice;
import org.openmrs.module.trumpmodule.patientassignment.PatientAssignment;
import org.openmrs.module.trumpmodule.provenance.Provenance;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.Collection;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.tdb.TDBFactory;

@Resource(name = "v1/trumpmodule/patientassignment", supportedClass = PatientAssignment.class, supportedOpenmrsVersions = {
		"1.8.*", "1.9.*" })
public class PatientAssignmentResource extends
		DataDelegatingCrudResource<PatientAssignment> {
	
	private static final String CREATE_ASSIGNMENT = "Create Assignment";
	private static final String DELETE_ASSIGNMENT = "Delete Assignment";
	private static final String UPDATE_ASSIGNMENT = "Update Assignment";
	private static final String VIEW_ASSIGNMENT = "View Assignment";

	
	private Dataset dataset;
	OpenmrsEnforceServiceContext openmrsContext = OpenmrsEnforceServiceContext
			.getInstance();
	private String directory = openmrsContext.getProvenanceDirectory();
	private Provenance pro = new Provenance();

	@Override
	public DelegatingResourceDescription getRepresentationDescription(
			Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("pauuid");
			description.addProperty("patientUUID");
			description.addProperty("assigned_user_id");
			description.addProperty("invalidated");
			description.addProperty("display", findMethod("getDisplayString"));
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		} else if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("pauuid");
			description.addProperty("assigned_user_id");
			description.addProperty("patientUUID");
			description.addProperty("userId");
			description.addProperty("invalidated");
			description.addProperty("display", findMethod("getDisplayString"));

			description.addProperty("auditInfo", findMethod("getAuditInfo"));
			description.addSelfLink();
			return description;
		}
		return null;
	}

	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addRequiredProperty("assigned_user_id");
		description.addRequiredProperty("patientUUID");
		return description;
	}

	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addRequiredProperty("assigned_user_id");
		description.addRequiredProperty("patientUUID");

		return description;
	}

	@Override
	public List<Representation> getAvailableRepresentations() {
		return Arrays.asList(Representation.DEFAULT);
	}

	@Override
	public PatientAssignment save(PatientAssignment delegate) {
		
		// every method will need to define the priveleges required, and also
		// make the call the the access control code
		// these three lines will appear in every method we want to check access for
		Collection<String> requiredPrivileges = new ArrayList<String>();
		requiredPrivileges.add(PatientAssignmentResource.CREATE_ASSIGNMENT);
		requiredPrivileges.add(PatientAssignmentResource.UPDATE_ASSIGNMENT);
		
		checkAccessRequest("savePatientAssignment",new Object[]{delegate}, requiredPrivileges);
		
		HashMap<String,String> properties = new HashMap<String,String>();
		properties.put(ProvenanceStrings.PATIENT_UUID, delegate.getPatientUUID());
		properties.put(ProvenanceStrings.ASSIGNED_USER_ID, delegate.getAssigned_user_id());
		
		if (!pro.checkExist("patientassignment", properties)) {
			return (PatientAssignment) new ProvenanceAdvice().addToTDB("save",delegate,"save_patientassignment/"
					,"patientassignment/", properties);
		}else {
			throw new ResourceDoesNotSupportOperationException("This patient assignment already exist! ");
		}
		

	}
	
	@Override
	protected void delete(PatientAssignment delegate, String reason,
			RequestContext context) throws ResponseException {
		// Before deleting the patientAssignment, we need to firstly check TDB, 
		// to see if there is the patientAssignment and if it was invalidated already,
		// if so, we can not delete a not exist patientAssignment or an already deleted
		// one. 
		
		Collection<String> requiredPrivileges = new ArrayList<String>();
		requiredPrivileges.add(PatientAssignmentResource.DELETE_ASSIGNMENT);
		checkAccessRequest("deletePatientAssignment",new Object[]{delegate,reason,context}, requiredPrivileges);
		HashMap<String,String> properties = new HashMap<String,String>();
		properties.put(ProvenanceStrings.PATIENT_UUID, delegate.getPatientUUID());
		properties.put(ProvenanceStrings.ASSIGNED_USER_ID, delegate.getAssigned_user_id());
		if (pro.checkExist("patientassignment", properties)) {
			// when we do delete patientAssignment, we do not actually delete it
			// from TDB, we do invalidating this patientAssignment entity.
			new ProvenanceAdvice().addToTDB("delete",delegate,"delete_patientassignment/",
					"patientassignment/", new HashMap<String,String>());
		}else 
			//throw new ObjectNotFoundException();
			throw new ResourceDoesNotSupportOperationException("This patient assignment has already been invalidated or not exist! ");
	}
	
	@Override
	public void purge(PatientAssignment delegate, RequestContext context)
			throws ResponseException {
	}

	@Override
	public PatientAssignment newDelegate() {
		return new PatientAssignment();
	}

	@Override
	public PatientAssignment getByUniqueId(String uniqueId) {
		
		Collection<String> requiredPrivileges = new ArrayList<String>();
		requiredPrivileges.add(PatientAssignmentResource.VIEW_ASSIGNMENT);
		checkAccessRequest("searchPatientAssignment",new Object[]{uniqueId}, requiredPrivileges);

		PatientAssignment pa = new PatientAssignment();
		String patient_uuid = null;
		String assigned_user_id = null;
		boolean invalidated = false;
		
		HashMap<String,String> properties = new Provenance().getByUUID("patientassignment", uniqueId);
		
		if(properties.isEmpty()){
			return null;
		}else{
			for(String key : properties.keySet()){
				if (key.equals("assigned_user_id")){
					assigned_user_id = properties.get(key);
				}else if(key.equals("patient_uuid")){
					patient_uuid = properties.get(key);
				}else if(key.equals("isvalidated")){
					invalidated = true;
				}
			}
			// unlike saving everything to OpenmrsServiceContext class, we are getting information from TDB, 
			// which means we can't get the object, we can only get the information of the object, so we need
			// to new an instance and set the information by passing the obtained information to the new 
			// instance. Am I right ? --- YES
			pa.setAssigned_user_id(assigned_user_id);
			pa.setPatientUUID(patient_uuid);
			pa.setUuid(uniqueId);
			pa.setInvalidated(invalidated);
			pa.setUserId(Context.getAuthenticatedUser().getId().toString());
		}

		return pa;
	}

	@Override
	public NeedsPaging<PatientAssignment> doGetAll(RequestContext context) {
		Collection<String> requiredPrivileges = new ArrayList<String>();
		requiredPrivileges.add(PatientAssignmentResource.VIEW_ASSIGNMENT);
		checkAccessRequest("searchPatientAssignment",new Object[]{context}, requiredPrivileges);
		
		List<PatientAssignment> patientAssignments = new ArrayList<PatientAssignment>();
		List<String> uuidList = new Provenance().getAll("patientassignment");
		
		for(int i = 0; i<uuidList.size(); i++){
			patientAssignments.add(getByUniqueId(uuidList.get(i)));
		}

		return new NeedsPaging<PatientAssignment>(patientAssignments, context);
	}



	public String getDisplayString(PatientAssignment patientAssignment) {
		return "Patient : "+patientAssignment.getPatientUUID() + " assigned to This User whose id is : "
				+ patientAssignment.getAssigned_user_id() + ".  NOTE: This assignment is active  :" + !patientAssignment.getInvalidated();
	}
	
	
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		Collection<String> requiredPrivileges = new ArrayList<String>();
		requiredPrivileges.add(PatientAssignmentResource.VIEW_ASSIGNMENT);
		
		checkAccessRequest("searchPatientAssignment",new Object[]{context}, requiredPrivileges);
		
		List<PatientAssignment> paList = new ArrayList<PatientAssignment>(); //this list is the patientAssignment list which will be shown in the rest response.
		String includeInvalidated = context.getRequest().getParameter("include_invalidated");
		String q = null;
		
		// search specific patient assignment by the given assigned_user_id, this will return a list of patient assignment which involves 
		// the doctor.
		String assigned_user_id = context.getRequest().getParameter("assigned_user_id");
		
		if(assigned_user_id!=null){
			// if the parameter is true, means we want the invalidated patientassignment as well as the uninvalidated ones.
			// if the parameter is false, means we only want the uninvalidated ones.
			if(includeInvalidated.equals("true")){
				q = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" 
						+ "?pa NS:assigned_user_id " + "'"+assigned_user_id+"'" + " ."
						+ "?pa PROV:wasGeneratedBy ?assign_activity ."
						+ "?assign_activity NS:action_name 'save_patientassignment' ."
						+ "OPTIONAL { ?pa PROV:wasInvalidatedBy ?delete_patientassginment .}"
						+ "}";
			}else {
				q = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" 
						+ "?pa NS:assigned_user_id " + "'"+assigned_user_id+"'" + " ."
						+ "?pa PROV:wasGeneratedBy ?activity ."
						+ "?activity NS:action_name 'save_patientassignment' ."
						+ "FILTER NOT EXISTS { ?pa PROV:wasInvalidatedBy ?delete_patientassginment .}"
						+ "}";
			}
		}
		// search specific patient assignment by the given patientuuid, this will return a list of patient assignment which involves 
		// the patient.
		String patientUUID = context.getRequest().getParameter("patientuuid");
		if(patientUUID!=null){
			if(includeInvalidated.equals("true")){
				q = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" 
						+ "?pa NS:patient_uuid " + "'"+patientUUID+"'" + " ."
						+ "?pa PROV:wasGeneratedBy ?activity ."
						+ "?activity NS:action_name 'save_patientassignment' ."
						+ "OPTIONAL { ?pa PROV:wasInvalidatedBy ?delete_patientassginment .}"
						+ "}";
			}else {
				q = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" 
						+ "?pa NS:patient_uuid " + "'"+patientUUID+"'" + " ."
						+ "?pa PROV:wasGeneratedBy ?activity ."
						+ "?activity NS:action_name 'save_patientassignment' ."
						+ "FILTER NOT EXISTS { ?pa PROV:wasInvalidatedBy ?delete_patientassginment .}"
						+ "}";
			}
			
		}
		
		dataset = TDBFactory.createDataset(directory);
		Query query = QueryFactory.create(q);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		ResultSet results = qexec.execSelect();
		List<String> uuidList = new ArrayList<String>();
		
		while (results.hasNext()) {
			QuerySolution row = results.next();
			String things = row.get("pa").toString();
			String[] ss = things.split("/");
			String paUUID = ss[ss.length-1];
			uuidList.add(paUUID);
		}
		
		for(int i = 0; i<uuidList.size(); i++){
			paList.add(getByUniqueId(uuidList.get(i)));
		}
		
		if (paList == null)
				return new EmptySearchResult();
		
		return new NeedsPaging<PatientAssignment>(paList, context);
	}
	
	/**
	 * Checks access requests for methods on this class using XACML layer
	 * @param args
	 * @param requiredPrivileges
	 * @param method
	 */
	private void checkAccessRequest(String methodName,Object[] args, Collection<String> requiredPrivileges) {
		try {
			new AuthorizationAdvice().checkAccessRequest(methodName,args, requiredPrivileges, true, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
}