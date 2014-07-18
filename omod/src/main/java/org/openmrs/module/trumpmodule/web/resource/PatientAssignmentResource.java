package org.openmrs.module.trumpmodule.web.resource;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import luca.tmac.basic.data.uris.ProvenanceStrings;

import org.openmrs.OpenmrsData;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.patientassignment.PatientAssignment;
import org.openmrs.module.trumpmodule.policies.Policy;
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
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.tdb.TDBFactory;


@Resource(name ="v1/trumpmodule/patientassignment", supportedClass = PatientAssignment.class, supportedOpenmrsVersions = {"1.8.*", "1.9.*"})
public class PatientAssignmentResource extends DataDelegatingCrudResource<PatientAssignment> {


	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("patientName");
			description.addProperty("doctorId");
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		}else if (rep instanceof FullRepresentation){
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("patientName");
			description.addProperty("doctorId");
			description.addProperty("patientUUID");
			description.addProperty("userId");

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
		//description.addRequiredProperty("patientName");
		description.addRequiredProperty("patientUUID");
		//description.addRequiredProperty("userId");
		return description;
	}

	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addRequiredProperty("doctorId");
		//description.addRequiredProperty("patientName");
		description.addRequiredProperty("patientUUID");
		//description.addRequiredProperty("userId");

		return description;
	}

	@Override
	public List<Representation> getAvailableRepresentations() {
		return Arrays.asList(Representation.DEFAULT);
	}


	@Override
	public PatientAssignment save(PatientAssignment delegate) {
		OpenmrsEnforceServiceContext openmrsContext = OpenmrsEnforceServiceContext.getInstance();
		String directory = openmrsContext.getProvenanceDirectory();
		Dataset dataset= TDBFactory.createDataset(directory);

		long startTime = System.currentTimeMillis();
		//Model model = dataset.getDefaultModel();
//		GraphStore graphStore = GraphStoreFactory.create(dataset);
//		UpdateRequest request = UpdateFactory.create();
//		String subject = delegate.getUserId();
//		String predicate = "rdf:type";
		ProvenanceBundle provBundle = new ProvenanceBundle(ProvenanceStrings.NS);

		String activityURI = provBundle.createActivity();
		com.hp.hpl.jena.rdf.model.Resource activity = provBundle.getResource(activityURI);
		// add statement describing when the activity started
		provBundle.addStartedAtTime(activity, startTime);


		// create a new action property, if it doesn't already exist, which is just the name of the invoked method
		Property actionProp = provBundle.getModel().createProperty(ProvenanceStrings.NS, ProvenanceStrings.ACTIVITY_NS);
		activity.addProperty(actionProp, "assignPatient");

		// agent - comes from the logged in user or the user who is invoking the method 
		User user = Context.getAuthenticatedUser();
		String agentURI = provBundle.createAgent(ProvenanceStrings.NS + ProvenanceStrings.AGENT + user.getUuid());
		com.hp.hpl.jena.rdf.model.Resource agent = provBundle.getResource(agentURI);

		String agent2URI = provBundle.createAgent(ProvenanceStrings.NS + ProvenanceStrings.AGENT + delegate.getDoctorId());
		com.hp.hpl.jena.rdf.model.Resource agent2 = provBundle.getResource(agent2URI);

		String agent3URI = provBundle.createAgent(ProvenanceStrings.NS + ProvenanceStrings.AGENT + delegate.getPatientUUID());
		com.hp.hpl.jena.rdf.model.Resource agent3 = provBundle.getResource(agent3URI);

		String entityURI = provBundle.createEntity(ProvenanceStrings.NS + ProvenanceStrings.ENTITY_PATIENT_ASSIGNMENT + delegate.getUuid());
		com.hp.hpl.jena.rdf.model.Resource entity = provBundle.getResource(entityURI);
		// record the relationship between the entity and the activity
		provBundle.addWasGeneratedBy(entity, activity);

		// record the relationship between the activity and the agent
		provBundle.addWasAssociatedWith(activity, agent);

		// record the relationship between the entity and the agent
		provBundle.addWasAttributedTo(entity, agent);


		// add statement describing when the activity ended.
		provBundle.addEndedAtTime(activity, System.currentTimeMillis());

		Model model = dataset.getDefaultModel();

		model.add(provBundle.getModel());

		dataset.close();

		return delegate;

	}

	@Override
	protected void delete(PatientAssignment delegate, String reason,
			RequestContext context) throws ResponseException {
		// TODO Auto-generated method stub

	}

	@Override
	public void purge(PatientAssignment delegate, RequestContext context)
			throws ResponseException {
		// TODO Auto-generated method stub

	}

	@Override
	public PatientAssignment newDelegate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PatientAssignment getByUniqueId(String uniqueId) {
		// TODO get from TDB
		return null;
	}

	@Override
	public NeedsPaging<PatientAssignment> doGetAll(RequestContext context){
		List<PatientAssignment> patientAssignments = new ArrayList<PatientAssignment>();

		return new NeedsPaging<PatientAssignment>(patientAssignments,context);
	}


	public String getDisplayString(PatientAssignment patientAssignment) {
		return patientAssignment.getPatientName() + " assigned to : " + patientAssignment.getDoctorId();
	}


}