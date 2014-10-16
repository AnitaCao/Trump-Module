package org.openmrs.module.trumpmodule.dataFinder;


import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.DoubleAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.tdb.TDBFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.xml.SubjectAttributeXmlName;
import luca.tmac.basic.data.uris.ProvenanceStrings;
import luca.tmac.basic.data.uris.SubjectAttributeURI;

import com.hp.hpl.jena.query.Dataset;


public class OpenmrsSubjectAttributeFinderModule extends AbstractAttributeFinderModule {
	
//	private URI defaultSubjectId;
	
	private Set<String> categories;
	private Set<String> ids;
	DataHandler data = null;
	User user;
	Object[] parameters = null;
	String methodName = null;
	
	public OpenmrsSubjectAttributeFinderModule(DataHandler pData, Object[] parameters, String methodName) {
		
//		try {
//			defaultSubjectId = new URI(user.getId().toString());
//		} catch (URISyntaxException e) {
//			// ignore
//		}
//		
		this.parameters = parameters;
		this.methodName = methodName;
		//set Data Handler
		this.data = pData;
		
		this.user = Context.getAuthenticatedUser();
	
		//System.out.println("Anita message for you: role=" + user.getAllRoles() + "userID : " + user.getId());
		//set supported categories
		categories = new HashSet<String>();
		categories.add(SubjectAttributeURI.SUBJECT_CATEGORY_URI);
		
		//set supported ids
		ids = new HashSet<String>();
		ids.add(SubjectAttributeURI.ASSIGNED_PATIENT_URI);
		ids.add(SubjectAttributeURI.SUBJECT_CATEGORY_URI);
		ids.add(SubjectAttributeURI.BUDGET_URI);
		ids.add(SubjectAttributeURI.ACTION_URI);
		ids.add(SubjectAttributeURI.RESOURCE_TYPE_URI);
		
	}

	@Override
	public Set<String> getSupportedCategories() {
		return categories;
	}

	@Override
	public Set<String> getSupportedIds() {
		return ids;
	}
	
	// find subject attributes from openmrs 
	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId,
			String issuer, URI category, EvaluationCtx context) {
		
		if(!getSupportedCategories().contains(category.toString()))
			return new EvaluationResult(getEmptyBag());
		
		AttributeValue AttResult = findAttributes(methodName, attributeId);

		return new EvaluationResult(AttResult);
	}

	private AttributeValue findAttributes(String methodName, URI attributeURI){
		String attribute = null;
		String attributeType = null;
		List<AttributeValue> values = new ArrayList<AttributeValue>();
		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		query.add(new AttributeQuery(SubjectAttributeXmlName.ID,user.getId().toString(),StringAttribute.identifier));
	
		BagAttribute bag = null; 
		
		
		if(attributeURI.toString().equals(SubjectAttributeURI.ROLE_URI))
		{
			
			//get current user roles
			Set<Role> userRoles = user.getRoles();
			
			//get the name of roles
			for(Role r : userRoles){
				values.add(StringAttribute.getInstance(r.getName()));
			}			
			try {
				bag = new BagAttribute(new URI(StringAttribute.identifier), values);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			
		}
		//this assigned-patient-uri should change to wanted patient uri, so we can get the assigned patients from the data file and compare them
		else if(attributeURI.toString().equals(SubjectAttributeURI.WANTED_PATIENT_URI))
		{	
			values.add(StringAttribute.getInstance(parameters[0].toString()));
			try {
				bag = new BagAttribute(new URI(StringAttribute.identifier), values);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			
			
		}else if(attributeURI.toString().equals(SubjectAttributeURI.ID_URI))
		{
			
			values.add(StringAttribute.getInstance(user.getId().toString()));
			try {
				bag = new BagAttribute(new URI(StringAttribute.identifier), values);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			
		}else if(attributeURI.toString().equals(SubjectAttributeURI.ASSIGNED_PATIENT_URI))
		{
			if (methodName.startsWith("getPatient")) {

				// TODO Get assigned patient uuid from TDB, not from
				// OpenmrsEnforceServiceContext.
				// So here, we need to select the patientuuid from TDB. We know
				// the doctorId (which is actually the userId, because current user is the one who
				// want to get the assigned patient information, so we need to check if the
				// wanted patient is assigned to this user or not), here we need to get the
				// patientuuid according to the doctorId.
				OpenmrsEnforceServiceContext openmrsContext = OpenmrsEnforceServiceContext.getInstance();
				String directory = openmrsContext.getProvenanceDirectory();
				Dataset dataset = TDBFactory.createDataset(directory);

				String queryString = ProvenanceStrings.QUERY_PREFIX
						+ "SELECT *" 
						+ "WHERE {" + "?pa NS:doctor_id " + "'"+user.getId().toString()+"'" + " ."
						+ "?pa NS:patient_uuid ?patient_uuid ." 
						+ "}";
				Query q = QueryFactory.create(queryString);
				QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
				ResultSet results = qexec.execSelect();
				// ResultSetFormatter.out(results);
				while (results.hasNext()) {
					String patient_uuid = results.next().get("patient_uuid")
							.toString();
					//System.out.println(patient_uuid);
					values.add(StringAttribute.getInstance(patient_uuid));
				}
				try {
					bag = new BagAttribute(new URI(StringAttribute.identifier), values);
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}else{
				attributeType = StringAttribute.identifier;
				attribute = SubjectAttributeXmlName.ASSIGNED_PATIENT;
				bag = data.getBagAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query, attribute, attributeType);
			}
			
		}else if(attributeURI.toString().equals(SubjectAttributeURI.BUDGET_URI))
		{
			attributeType = DoubleAttribute.identifier;
			attribute = SubjectAttributeXmlName.BUDGET;
			bag = data.getBagAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query, attribute, attributeType);
			
		}else if(attributeURI.toString().equals(SubjectAttributeURI.RESOURCE_TYPE_URI))
		{
			values.add(StringAttribute.getInstance("patientassignment"));
			try {
				bag = new BagAttribute(new URI(StringAttribute.identifier), values);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			
		}

		for(int i =0; i< values.size();i++){
			System.out.println("values is :  " + values.get(i).toString());
		}
		return bag;
	}
	
}
