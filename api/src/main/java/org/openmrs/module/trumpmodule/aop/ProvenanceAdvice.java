package org.openmrs.module.trumpmodule.aop;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.openmrs.OpenmrsData;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import uk.ac.dotrural.prov.jena.ProvenanceBundle;

public class ProvenanceAdvice implements MethodInterceptor {
	
	/**
	 * List of all method name prefixes that result in INFO-level log messages
	 */
	private static final String[] SETTER_METHOD_PREFIXES = {"create" };
	private static final String NS = "http://trump-india-uk.org/prov/";
	private static final String ACTIVITY_NS = "action";
	
	// entity types
	private static final String ENTITY_PATIENT = "patient";
	
	// agent namespace
	private static final String AGENT = "agent";

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		String name = method.getName();
		
		// decide what type of logging we're doing with the current method and loglevel
		boolean isSetterTypeOfMethod = OpenmrsUtil.stringStartsWith(name, SETTER_METHOD_PREFIXES);		
		
		// used for the execution time calculations
		long startTime = System.currentTimeMillis();
		
		ProvenanceBundle provBundle = new ProvenanceBundle(NS);
		
		if (isSetterTypeOfMethod) {
			String activityURI = provBundle.createActivity();
			Resource activity = provBundle.getResource(activityURI);
			
			// create a new action property, if it doesn't already exist, which is just the name of the invoked method
			Property actionProp = provBundle.getModel().createProperty(NS, ACTIVITY_NS);
			activity.addProperty(actionProp, name);
			
			// TODO we're not recording stuff from the method invocation YET
			// WE CAN DO THE FOLLOWING STEP BEFORE EXECUTING THE METHOD:
			
			// 1. agent - comes from the logged in user or the user who is invoking the method 
			User user = Context.getAuthenticatedUser();
			String agentURI = provBundle.createAgent(NS + AGENT + user.getUuid());
			Resource agent = provBundle.getResource(agentURI);
			
			// NOW: we need to execute the method to actually get the created entity details
			OpenmrsData result = null;
			try {
				result = (OpenmrsData) invocation.proceed();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// entity - comes from the uuid of the newly created patient (the entity is not an agent, it represents the new record)
			// we are recording the fact that some agent (i.e. the logged in user) is doing an activity (i.e. createPatient) which is resulting in some
			// entity being created (i.e. a new patient record with a UUID) - i.e. from the result we just got
			String entityURI = provBundle.createEntity(NS + ENTITY_PATIENT + result.getUuid());
			Resource entity = provBundle.getResource(entityURI);
			
			// record the relationship between the entity and the activity
			provBundle.addWasGeneratedBy(entity, activity);
			
			// record the relationship between the activity and the agent
			provBundle.addWasAssociatedWith(activity, agent);
			
			// record the relationship between the entity and the agent
			provBundle.addWasAttributedTo(entity, agent);
			
			
				
		//insert to TDB
		}
		
		
		return null;
	}
}
	


