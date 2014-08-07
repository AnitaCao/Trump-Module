package org.openmrs.module.trumpmodule.web.resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import luca.tmac.basic.data.uris.ProvenanceStrings;

import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.patientassignment.PatientAssignment;
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

import uk.ac.dotrural.prov.jena.ProvenanceBundle;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.tdb.TDBFactory;

@Resource(name = "v1/trumpmodule/patientassignment", supportedClass = PatientAssignment.class, supportedOpenmrsVersions = {
		"1.8.*", "1.9.*" })
public class PatientAssignmentResource extends
		DataDelegatingCrudResource<PatientAssignment> {

	private Dataset dataset;
	OpenmrsEnforceServiceContext openmrsContext = OpenmrsEnforceServiceContext
			.getInstance();
	private String directory = openmrsContext.getProvenanceDirectory();

	@Override
	public DelegatingResourceDescription getRepresentationDescription(
			Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("patientUUID");
			description.addProperty("doctorId");
			description.addProperty("invalidated");
			description.addProperty("display", findMethod("getDisplayString"));
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		} else if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("doctorId");
			description.addProperty("patientUUID");
			description.addProperty("userId");
			description.addProperty("patientassignmentUUID");
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
		description.addRequiredProperty("doctorId");
		description.addRequiredProperty("patientUUID");
		return description;
	}

	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addRequiredProperty("doctorId");
		description.addRequiredProperty("patientUUID");

		return description;
	}

	@Override
	public List<Representation> getAvailableRepresentations() {
		return Arrays.asList(Representation.DEFAULT);
	}

	@Override
	public PatientAssignment save(PatientAssignment delegate) {
		
		if (!checkExist(delegate.getDoctorId(),delegate.getPatientUUID())) {
			long startTime = System.currentTimeMillis();
			dataset = TDBFactory.createDataset(directory);
			ProvenanceBundle provBundle = new ProvenanceBundle(ProvenanceStrings.NS);
	
			// insert to TDB
			// 1. the activity has an property : action_name
			String activityURI = provBundle.createActivity(ProvenanceStrings.NS
					+ ProvenanceStrings.ACTIVITY_ASSIGN_PATIENT
					+ UUID.randomUUID().getMostSignificantBits());
			com.hp.hpl.jena.rdf.model.Resource activity = provBundle.getResource(activityURI);
	
			Property actionProp = provBundle.getModel().createProperty(
					ProvenanceStrings.NS, ProvenanceStrings.ACTIVITY_NAME);
			activity.addProperty(actionProp, "assign_patient");
	
			// 2. agent - comes from the logged in user or the user who is invoking the method
			String agentURI = provBundle.createAgent(ProvenanceStrings.NS
					+ ProvenanceStrings.AGENT_USER + delegate.getUserId());
			com.hp.hpl.jena.rdf.model.Resource agent = provBundle
					.getResource(agentURI);
	
			// 3. the entity is patientAssignment
			String entityURI = provBundle.createEntity(ProvenanceStrings.NS
					+ ProvenanceStrings.ENTITY_PATIENT_ASSIGNMENT
					+ delegate.getPatientassignmentUUID());
			com.hp.hpl.jena.rdf.model.Resource entity = provBundle.getResource(entityURI);
	
			// patientAssignment entity has 2 property :  patient_uuid
			// and doctor_id
	
			Property entityProp2 = provBundle.getModel().createProperty(
					ProvenanceStrings.NS, ProvenanceStrings.PATIENT_UUID);
			entity.addProperty(entityProp2, delegate.getPatientUUID());
	
			Property entityProp3 = provBundle.getModel().createProperty(
					ProvenanceStrings.NS, ProvenanceStrings.DOCTOR_ID);
			entity.addProperty(entityProp3, delegate.getDoctorId());
	
			// add statement describing when the activity started
			provBundle.addStartedAtTime(activity, startTime);
			// add statement describing when the activity ended.
			provBundle.addEndedAtTime(activity, System.currentTimeMillis());
	
			// the activity was started by the agent.
			provBundle.addWasStartedBy(activity, agent);
			// the entity was generated by the activity
			provBundle.addWasGeneratedBy(entity, activity);
			// the entity was attributed to the agent
			provBundle.addWasAttributedTo(entity, agent);
	
			provBundle.getModel().write(System.out);
	
			Model model = dataset.getDefaultModel();
	
			model.add(provBundle.getModel());
	
			dataset.close();
			return delegate;
		}else {
			throw new ResourceDoesNotSupportOperationException("This patient assignment already exist! ");
		}
		

	}

	@Override
	protected void delete(PatientAssignment delegate, String reason,
			RequestContext context) throws ResponseException {
		// TODO Before deleting the patientAssignment, we need to firstly check TDB, 
		// to see if there is the patientAssignment and if it was invalidated already,
		// if so, we can not delete an un-exist patientAssignment or an already deleted
		// one. 
		
		System.err.println("the patientassignment uuid is:"+ delegate.getPatientassignmentUUID());
		System.out.println("the patient uuid is:"+ delegate.getPatientUUID());
		
		if (checkExist(delegate.getDoctorId(),delegate.getPatientUUID())) {
			// when we do delete patientAssignment, we do not actually delete it
			// from TDB, we do invalidating this patientAssignment entity.
			
			long startTime = System.currentTimeMillis();
			dataset = TDBFactory.createDataset(directory);
			ProvenanceBundle provBundle = new ProvenanceBundle(ProvenanceStrings.NS);

			// insert to TDB
			// 1. the activity has an property : action_name
			String activityURI = provBundle.createActivity(ProvenanceStrings.NS
					+ ProvenanceStrings.ACTIVITY_UNASSIGN_PATIENT
					+ UUID.randomUUID().getMostSignificantBits());
			com.hp.hpl.jena.rdf.model.Resource activity = provBundle.getResource(activityURI);

			Property actionProp = provBundle.getModel().createProperty(
					ProvenanceStrings.NS, ProvenanceStrings.ACTIVITY_NAME);
			activity.addProperty(actionProp, "unassign_patient");

			// 2. agent - comes from the logged in user or the user who is
			// invoking the method
			String agentURI = provBundle.createAgent(ProvenanceStrings.NS
					+ ProvenanceStrings.AGENT_USER + delegate.getUserId());
			com.hp.hpl.jena.rdf.model.Resource agent = provBundle.getResource(agentURI);

			// 3. the entity is patientAssignment
			// here we need to get the patientAssignment entity from TDB according
			// to the uuid of the patientAssignment which we want to delete.
			String entityURI = provBundle.createEntity(ProvenanceStrings.NS
					+ ProvenanceStrings.ENTITY_PATIENT_ASSIGNMENT
					+ delegate.getPatientassignmentUUID());
			com.hp.hpl.jena.rdf.model.Resource entity = provBundle.getResource(entityURI);

			// add statement describing when the activity started
			provBundle.addStartedAtTime(activity, startTime);
			// add statement describing when the activity ended.
			provBundle.addEndedAtTime(activity, System.currentTimeMillis());
			// the activity was started by the agent.
			provBundle.addWasStartedBy(activity, agent);

			provBundle.addWasInvalidatedBy(entity, activity);

			provBundle.getModel().write(System.out);

			Model model = dataset.getDefaultModel();

			model.add(provBundle.getModel());

			dataset.close();
		}else 
			//throw new ObjectNotFoundException();
			throw new ResourceDoesNotSupportOperationException("This patient assignment has already been invalidated or not exist! ");
		
	}

	@Override
	public void purge(PatientAssignment delegate, RequestContext context)
			throws ResponseException {
		// TODO Auto-generated method stub
	}

	@Override
	public PatientAssignment newDelegate() {
		return new PatientAssignment();
	}

	@Override
	public PatientAssignment getByUniqueId(String uniqueId) {

		String patient_uuid = null;
		String doctor_id = null;
		boolean invalidated = false;

		String queryString = ProvenanceStrings.QUERY_PREFIX + "SELECT *  WHERE {"
				+ "pA:" +uniqueId + " ?property ?value}";

		System.err.println(queryString);
		dataset = TDBFactory.createDataset(directory);
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		ResultSet results = qexec.execSelect();
		while (results.hasNext()) {

			QuerySolution row = results.next();

			Iterator columns = row.varNames();

			while (columns.hasNext()) {

				RDFNode cell = row.get((String) columns.next());

				if (cell.isResource()) {
					com.hp.hpl.jena.rdf.model.Resource resource = cell
							.asResource();
					String resourceString = resource.toString();
					if (resourceString.contains("doctor_id")) {
						doctor_id = row.get((String) columns.next()).toString();

					} else if (resourceString.contains("patient_uuid")) {
						patient_uuid = row.get((String) columns.next()).toString();

					} else if (resourceString.contains("wasInvalidatedBy")){
						invalidated = true;
					}
				} else {
					System.out.println(cell.toString());
				}
			}
		}

		// unlike saving everything to OpenmrsServiceContext class, we are getting information from TDB, 
		// which means we can't get the object, we can only get the information of the object, so we need
		// to new an instance and set the information by passing the obtained information to the new 
		// instance. Am I right ? --- YES
		PatientAssignment pa = new PatientAssignment();
		pa.setDoctorId(doctor_id);
		pa.setPatientUUID(patient_uuid);
		pa.setPatientassignmentUUID(uniqueId);
		pa.setInvalidated(invalidated);
		pa.setUserId(Context.getAuthenticatedUser().getId().toString());
		pa.setInvalidated(invalidated);

		dataset.close();
		return pa;
	}

	@Override
	public NeedsPaging<PatientAssignment> doGetAll(RequestContext context) {
		List<PatientAssignment> patientAssignments = new ArrayList<PatientAssignment>();
		List<String> uuidList = new ArrayList<String>();

		String q = ProvenanceStrings.QUERY_PREFIX + "SELECT ?s " + "WHERE {"+ "?s a PROV:Entity .}";

		dataset = TDBFactory.createDataset(directory);
		Query query = QueryFactory.create(q);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		ResultSet results = qexec.execSelect();
		
		while (results.hasNext()) {
			QuerySolution row = results.next();
			String things = row.get("s").toString();
			if (things.contains("patientassignment")) {
				String[] ss = things.split("/");
				String paUUID = ss[ss.length-1];
				System.out.println(paUUID);
				uuidList.add(paUUID);
			}
		}
		dataset.close();
		
		for(int i = 0; i<uuidList.size(); i++){
			patientAssignments.add(getByUniqueId(uuidList.get(i)));
		}

		return new NeedsPaging<PatientAssignment>(patientAssignments, context);
	}

	public String getDisplayString(PatientAssignment patientAssignment) {
		return "Patient : "+patientAssignment.getPatientUUID() + " assigned to Doctor: "
				+ patientAssignment.getDoctorId() + ".  NOTE: This assignment is active  :" + !patientAssignment.getInvalidated();
	}
	
	public boolean checkExist(String doctorId, String patientUUID){
		boolean exist = false;
		boolean isInValidated = false;
		
		String q = ProvenanceStrings.QUERY_PREFIX + "SELECT *  WHERE {"
				+ "?pa NS:doctor_id "+"'"+doctorId+"' ."
        		+ "?pa NS:patient_uuid "+"'"+patientUUID+"' ."
        		+ "?pa PROV:wasGeneratedBy ?assign_activity ."
        		+ "?assign_activity PROV:startedAtTime ?assign_time ."
        		+ "OPTIONAL { ?pa PROV:wasInvalidatedBy ?unassign_activity ."
        		+            "?unassign_activity PROV:startedAtTime ?unassign_time .}"
        		+ "}";
		dataset = TDBFactory.createDataset(directory);
		Query query = QueryFactory.create(q);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		ResultSet results = qexec.execSelect();
		while(results.hasNext()){
			exist = true;
			QuerySolution row = results.next();
			RDFNode unassignTimeNode = row.get("unassign_time");
			if(unassignTimeNode!=null){
				String unassignTime = unassignTimeNode.toString();
				String assignTime = row.get("assign_time").toString();
				System.err.println("the unassign_time is : " + unassignTime);
				System.err.println("the assign_time is : " + assignTime);
				System.out.println(unassignTime.compareTo(assignTime));
				if(unassignTime.compareTo(assignTime)>=0){ //means unassign_activity happened at a later time
					                                       //which means it has been unassigned.
					isInValidated = true;
				}
			}else isInValidated = false;
		}
		return (exist&&!isInValidated);
	}
	
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		List<PatientAssignment> paList = new ArrayList<PatientAssignment>();
		String containInvalidated = context.getRequest().getParameter("contain_invalidated");
		String q = null;
		// search specific patient assignment by the given doctorid, this will return a list of patient assignment which involves 
		// the doctor.
		String doctorId = context.getRequest().getParameter("doctorid");
		
		if(doctorId!=null){
			// if the parameter is true, means we want the invalidated patientassignment as well as the uninvalidated ones.
			// if the parameter is false, means we only want the uninvalidated ones.
			if(containInvalidated.equals("true")){
				q = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" 
						+ "?pa NS:doctor_id " + "'"+doctorId+"'" + " ."
						+ "?pa PROV:wasGeneratedBy ?activity ."
						+ "?activity NS:action_name 'assign_patient' ."
						+ "OPTIONAL { ?pa PROV:wasInvalidatedBy ?unassign_activity .}"
						+ "}";
			}else {
				q = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" 
						+ "?pa NS:doctor_id " + "'"+doctorId+"'" + " ."
						+ "?pa PROV:wasGeneratedBy ?activity ."
						+ "?activity NS:action_name 'assign_patient' ."
						+ "FILTER NOT EXISTS { ?pa PROV:wasInvalidatedBy ?unassign_activity .}"
						+ "}";
			}
		}
		// search specific patient assignment by the given patientuuid, this will return a list of patient assignment which involves 
		// the patient.
		String patientUUID = context.getRequest().getParameter("patientuuid");
		if(patientUUID!=null){
			if(containInvalidated.equals("true")){
				q = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" 
						+ "?pa NS:patient_uuid " + "'"+patientUUID+"'" + " ."
						+ "?pa PROV:wasGeneratedBy ?activity ."
						+ "?activity NS:action_name 'assign_patient' ."
						+ "OPTIONAL { ?pa PROV:wasInvalidatedBy ?unassign_activity .}"
						+ "}";
			}else {
				q = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" 
						+ "?pa NS:patient_uuid " + "'"+patientUUID+"'" + " ."
						+ "?pa PROV:wasGeneratedBy ?activity ."
						+ "?activity NS:action_name 'assign_patient' ."
						+ "FILTER NOT EXISTS { ?pa PROV:wasInvalidatedBy ?unassign_activity .}"
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
	
}