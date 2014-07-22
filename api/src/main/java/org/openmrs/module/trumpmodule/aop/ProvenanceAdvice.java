package org.openmrs.module.trumpmodule.aop;

import java.lang.reflect.Method;

import luca.tmac.basic.data.uris.ProvenanceStrings;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.openmrs.OpenmrsData;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.patientassignment.PatientAssignment;
import org.openmrs.util.OpenmrsUtil;

import uk.ac.dotrural.prov.jena.ProvenanceBundle;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;


public class ProvenanceAdvice implements MethodInterceptor {
	
	/**
	 * List of all method name prefixes that result in INFO-level log messages
	 */
//	private static final String[] SETTER_METHOD_PREFIXES = {"create" ,"save","void","delete"};
//	private static final String NS = "http://trump-india-uk.org/prov/";
//	private static final String ACTIVITY_NS = "action";
	
	// entity types
//	private static final String ENTITY_PATIENT = "patient/";
	
	// agent namespace
//	private static final String AGENT = "agent/";
	
	private Dataset dataset;
	
	OpenmrsEnforceServiceContext openmrsContext = OpenmrsEnforceServiceContext.getInstance();
	private String directory = openmrsContext.getProvenanceDirectory();

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		String name = method.getName();
		// data object resulting from this captured method invocation that we will store provenance about
		Object result = null;
		// decide what type of logging we're doing with the current method and loglevel
		if (OpenmrsUtil.stringStartsWith(name, ProvenanceStrings.SETTER_METHOD_PREFIXES)) {
			// used for the execution time calculations
			long startTime = System.currentTimeMillis();
			
			//System.out.println("the directory of provenance is : " + directory);
			dataset = TDBFactory.createDataset(directory);
			
			ProvenanceBundle provBundle = new ProvenanceBundle(ProvenanceStrings.NS);
			//insert to TDB
			
			String activityURI = provBundle.createActivity();
			Resource activity = provBundle.getResource(activityURI);
			
			// add statement describing when the activity started
			provBundle.addStartedAtTime(activity, startTime);

			// agent - comes from the logged in user or the user who is invoking the method
			User user = Context.getAuthenticatedUser();
			String agentURI = provBundle.createAgent(ProvenanceStrings.NS
					+ ProvenanceStrings.AGENT + user.getId());
			Resource agent = provBundle.getResource(agentURI);
			
			provBundle.addWasStartedBy(activity, agent); 

			// NOW: we need to execute the method to actually get the created
			// entity details
			result = (OpenmrsData) invocation.proceed();
			
			//if(result instanceof Patient){
				
				// create a new action property, if it doesn't already exist, which is just the name of the invoked method
				Property actionProp = provBundle.getModel().createProperty(
						ProvenanceStrings.NS, ProvenanceStrings.ACTIVITY_NS);
				activity.addProperty(actionProp, name);

				// entity - comes from the uuid of the newly created patient (the  entity is not an agent, it represents the new record)
				// we are recording the fact that some agent (i.e. the logged in user) is doing an activity (i.e. createPatient) which is
				// resulting in some entity being created (i.e. a new patient record with a UUID) -
				// i.e. from the result we just got
				String entityURI = provBundle.createEntity(ProvenanceStrings.NS
						+ ProvenanceStrings.ENTITY_PATIENT
						+ ((OpenmrsData) result).getUuid());
				Resource entity = provBundle.getResource(entityURI);
	
				// record the relationship between the entity and the activity
				provBundle.addWasGeneratedBy(entity, activity);
	
				// record the relationship between the activity and the agent
				//provBundle.addWasAssociatedWith(activity, agent);
				
				
				// record the relationship between the entity and the agent
				provBundle.addWasAttributedTo(entity, agent);
			
//			}else if (result instanceof PatientAssignment){
//				
//				PatientAssignment pa = (PatientAssignment)result;
//				
//				Property actionProp = provBundle.getModel().createProperty( ProvenanceStrings.NS, ProvenanceStrings.ACTIVITY_NS);
//				activity.addProperty(actionProp, "do_patient_assignment");
//				
//				String entityURI = provBundle.createEntity(ProvenanceStrings.NS
//						+ ProvenanceStrings.ENTITY_PATIENT_ASSIGNMENT
//						+ ((PatientAssignment) result).getUuid());
//				Resource entity = provBundle.getResource(entityURI);
//				
//				Property entityProp = provBundle.getModel().createProperty(ProvenanceStrings.NS, ProvenanceStrings.PATIENT_NAME);
//				entity.addProperty(entityProp, pa.getPatientName());
//				
////				Property entityProp1 = provBundle.getModel().createProperty(ProvenanceStrings.NS, ProvenanceStrings.DOCTOR_ID);
////				entity.addProperty(entityProp1, pa.getDoctorId());
////				
////				Property entityProp2 = provBundle.getModel().createProperty(ProvenanceStrings.NS, ProvenanceStrings.PATIENT_UUID);
////				entity.addProperty(entityProp2, pa.getPatientUUID());
//				
//				String agentURI1 = provBundle.createAgent(ProvenanceStrings.NS + ProvenanceStrings.AGENT + pa.getDoctorId());
//				Resource agent1 = provBundle.getResource(agentURI1);
//				String agentURI2 = provBundle.createAgent(ProvenanceStrings.NS + ProvenanceStrings.AGENT + pa.getPatientUUID());
//				Resource agent2 = provBundle.getResource(agentURI2);
//				
//				provBundle.addWasAssignedTo(agent1, agent2);
//				
//			}

			// add statement describing when the activity ended.
			provBundle.addEndedAtTime(activity, System.currentTimeMillis());

			provBundle.getModel().write(System.out);

			Model model = dataset.getDefaultModel();

			model.add(provBundle.getModel());

			dataset.close();
			
		} else {
			// if we're not interested in capturing provenance for this method, still let it proceed
			result = invocation.proceed();
		}
		return result;
		
	}
}
	


