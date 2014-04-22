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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.xml.SubjectAttributeXmlName;
import luca.tmac.basic.data.uris.SubjectAttributeURI;


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
				// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}else if(attributeURI.toString().equals(SubjectAttributeURI.ID_URI))
		{
			
			values.add(StringAttribute.getInstance(user.getId().toString()));
			try {
				bag = new BagAttribute(new URI(StringAttribute.identifier), values);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}else if(attributeURI.toString().equals(SubjectAttributeURI.ASSIGNED_PATIENT_URI))
		{
			if(methodName.equalsIgnoreCase("getPatient")){
				HashMap<String, HashSet<String>> assigendPatientIds = 
						OpenmrsEnforceServiceContext.getInstance().getAssigendPatientInternalIds();
				HashSet<String> set = assigendPatientIds.get(user.getId().toString());
				for(String ss : set){
					values.add(StringAttribute.getInstance(ss));
				}
				try {
					bag = new BagAttribute(new URI(StringAttribute.identifier), values);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
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
		}
		return bag;
	}
	
}
