package org.openmrs.module.trumpmodule.web.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import luca.tmac.basic.data.uris.ProvenanceStrings;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.patientassignment.PatientAssignment;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
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
			description.addSelfLink();
			description.addLink("full", ".?v="
					+ RestConstants.REPRESENTATION_FULL);
			return description;
		} else if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("patientName");
			description.addProperty("doctorId");
			description.addProperty("patientUUID");
			description.addProperty("userId");
			description.addProperty("patientassignmentUUID");

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

		Patient patient = Context.getPatientService().getPatientByUuid(delegate.getPatientUUID());

		String patientName = patient.getGivenName()+" "+patient.getMiddleName()+" " +patient.getFamilyName();

		delegate.setPatientName(patientName);
		
		long startTime = System.currentTimeMillis();
		dataset = TDBFactory.createDataset(directory);
		ProvenanceBundle provBundle = new ProvenanceBundle(ProvenanceStrings.NS);

		//delegate.setPatientName("DAVE"); //just for test.
		// insert to TDB

		// 1. the activity has an property : action_name
		String activityURI = provBundle.createActivity(ProvenanceStrings.NS
				+ ProvenanceStrings.ACTIVITY_PATIENT_ASSIGNMENT
				+ UUID.randomUUID().getMostSignificantBits());
		com.hp.hpl.jena.rdf.model.Resource activity = provBundle
				.getResource(activityURI);

		Property actionProp = provBundle.getModel().createProperty(
				ProvenanceStrings.NS, ProvenanceStrings.ACTIVITY_NAME);
		activity.addProperty(actionProp, "do_patient_assignment");

		// 2. agent - comes from the logged in user or the user who is invoking
		// the method
		String agentURI = provBundle.createAgent(ProvenanceStrings.NS
				+ ProvenanceStrings.AGENT_USER + delegate.getUserId());
		com.hp.hpl.jena.rdf.model.Resource agent = provBundle
				.getResource(agentURI);

		// 3. the entity is patientAssignment
		String entityURI = provBundle.createEntity(ProvenanceStrings.NS
				+ ProvenanceStrings.ENTITY_PATIENT_ASSIGNMENT
				+ delegate.getId());
		com.hp.hpl.jena.rdf.model.Resource entity = provBundle
				.getResource(entityURI);

		// patientAssignment entity has 3 property : patient_name, patient_uuid
		// and doctor_id
		Property entityProp1 = provBundle.getModel().createProperty(
				ProvenanceStrings.NS, ProvenanceStrings.PATIENT_NAME);
		entity.addProperty(entityProp1, delegate.getPatientName());

		Property entityProp2 = provBundle.getModel().createProperty(
				ProvenanceStrings.NS, ProvenanceStrings.PATIENT_UUID);
		entity.addProperty(entityProp2, delegate.getPatientUUID());

		Property entityProp3 = provBundle.getModel().createProperty(
				ProvenanceStrings.NS, ProvenanceStrings.DOCTOR_ID);
		entity.addProperty(entityProp3, delegate.getDoctorId());

		Property entityProp4 = provBundle.getModel()
				.createProperty(ProvenanceStrings.NS,
						ProvenanceStrings.PATIENT_ASSIGNMENT_UUID);
		entity.addProperty(entityProp4, delegate.getUuid());

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

	}

	@Override
	protected void delete(PatientAssignment delegate, String reason,
			RequestContext context) throws ResponseException {
		// when we do delete patientAssignment, we do not actually delete it from TDB, 
		// we do invalidating this patientAssignment entity.
		
		long startTime = System.currentTimeMillis();
		dataset = TDBFactory.createDataset(directory);
		ProvenanceBundle provBundle = new ProvenanceBundle(ProvenanceStrings.NS);

		// insert to TDB
		// 1. the activity has an property : action_name
		String activityURI = provBundle.createActivity(ProvenanceStrings.NS
				+ ProvenanceStrings.ACTIVITY_UNDO_PATIENT_ASSIGNMENT
				+ UUID.randomUUID().getMostSignificantBits());
		com.hp.hpl.jena.rdf.model.Resource activity = provBundle
				.getResource(activityURI);

		Property actionProp = provBundle.getModel().createProperty(
				ProvenanceStrings.NS, ProvenanceStrings.ACTIVITY_NAME);
		activity.addProperty(actionProp, "undo_patient_assignment");

		// 2. agent - comes from the logged in user or the user who is invoking
		// the method
		String agentURI = provBundle.createAgent(ProvenanceStrings.NS
				+ ProvenanceStrings.AGENT_USER + delegate.getUserId());
		com.hp.hpl.jena.rdf.model.Resource agent = provBundle
				.getResource(agentURI);

		// 3. the entity is patientAssignment
		// here we need to get the patientAssignment entity from TDB according 
		// to the uuid of the patientAssignment which we want to delete.
		String entityURI = provBundle.createEntity(ProvenanceStrings.NS
				+ ProvenanceStrings.ENTITY_PATIENT_ASSIGNMENT + delegate.getId());
		com.hp.hpl.jena.rdf.model.Resource entity = provBundle
				.getResource(entityURI);

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

		String patient_name = null;
		String patient_uuid = null;
		String doctor_id = null;
		String patientassignment_uuid = null;

		String queryString = ProvenanceStrings.QUERY_PREFIX + "SELECT ?property ?value " + "WHERE {"
				+ "?pa NS:patientassignment_uuid " + "'"+uniqueId+"'" + " . "
				+ "?pa ?property ?value}";

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
						// System.out.println("doctor_id is :" + doctor_id);

					} else if (resourceString.contains("patient_uuid")) {
						patient_uuid = row.get((String) columns.next())
								.toString();
						// System.out.println("patient_uuid is :" +
						// patient_uuid);

					} else if (resourceString.contains("patient_name")) {
						patient_name = row.get((String) columns.next())
								.toString();
						// System.out.println("patient_name is :"+
						// patient_name);
					} else if (resourceString
							.contains("patientassignment_uuid")) {
						patientassignment_uuid = row.get(
								(String) columns.next()).toString();
						// System.out.println("patientassignment_uuid is :"+
						// patientassignment_uuid);

					}
				} else {
					System.out.println(cell.toString());
				}
			}
		}

		// unlike saving everything to OpenmrsServiceContext class, we are getting information from TDB, 
		// which means we can't get the object, we can only get the information of the object, so we need
		// to new an instance and set the information by passing the obtained information to the new 
		// instance. Am I right ?
		PatientAssignment pa = new PatientAssignment();
		pa.setDoctorId(doctor_id);
		pa.setPatientName(patient_name);
		pa.setPatientUUID(patient_uuid);
		pa.setUuid(patientassignment_uuid);
		pa.setUserId(Context.getAuthenticatedUser().getId().toString());

		// ResultSetFormatter.out(results);
		return pa;
	}

	@Override
	public NeedsPaging<PatientAssignment> doGetAll(RequestContext context) {
		List<PatientAssignment> patientAssignments = new ArrayList<PatientAssignment>();
		List<String> uuidList = new ArrayList<String>();

		String queryString = ProvenanceStrings.QUERY_PREFIX + "SELECT ?s " + "WHERE {"
				+ "?s a PROV:Entity .}";

		dataset = TDBFactory.createDataset(directory);
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		ResultSet results = qexec.execSelect();
		
		List<String> lists = new ArrayList<String>();
		
		while (results.hasNext()) {
			QuerySolution row = results.next();
			String things = row.get("s").toString();
			if (things.contains("patientassignment")) {
				lists.add(things);
			}
		}

		for (int i = 0; i < lists.size(); i++) {
			String queryString2 = ProvenanceStrings.QUERY_PREFIX + "SELECT *" + "WHERE {" + "<"
					+ lists.get(i) + "> NS:patientassignment_uuid ?value .}";
			Query query2 = QueryFactory.create(queryString2);
			QueryExecution qexec2 = QueryExecutionFactory.create(query2,dataset);
			ResultSet results2 = qexec2.execSelect();

			while (results2.hasNext()) {
				QuerySolution row = results2.next();
				RDFNode thing = row.get("value");
				uuidList.add(thing.toString());
			}
		}
		
		for(int i = 0; i<uuidList.size(); i++){
			patientAssignments.add(getByUniqueId(uuidList.get(i)));
		}

		return new NeedsPaging<PatientAssignment>(patientAssignments, context);
	}

	public String getDisplayString(PatientAssignment patientAssignment) {
		return patientAssignment.getPatientName() + " assigned to : "
				+ patientAssignment.getDoctorId();
	}

}