package org.openmrs.module.trumpmodule.dataFinder;


import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.uris.PermissionAttributeURI;
import luca.tmac.basic.data.xml.SubjectAttributeXmlName;



public class OpenmrsPermissionAttributeFinderModule extends AbstractAttributeFinderModule {
	
	
	private Set<String> categories;
	private Set<String> ids;
	DataHandler data = null;
	User user;
	Object[] parameters = null;
	String methodName = null;
	
	public OpenmrsPermissionAttributeFinderModule(DataHandler pData, Object[] parameters, String methodName) {
		this.parameters = parameters;
		this.methodName = methodName;
		//set Data Handler
		this.data = pData;
		
		this.user = Context.getAuthenticatedUser();
	
		System.out.println("Anita message for you: role=" + user.getAllRoles() + "userID : " + user.getId());
		//set supported categories
		categories = new HashSet<String>();
		categories.add(PermissionAttributeURI.PERMISSION_CATEGORY_URI);
		
		//set supported ids
		ids = new HashSet<String>();
		ids.add(PermissionAttributeURI.RESOURCE_TYPE_URI);
		ids.add(PermissionAttributeURI.ACTION_URI);
	}

	@Override
	public Set<String> getSupportedCategories() {
		return categories;
	}

	@Override
	public Set<String> getSupportedIds() {
		return ids;
	}
	
	// find permission attributes from openmrs 
	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId,
			String issuer, URI category, EvaluationCtx context) {
		
		if(!getSupportedCategories().contains(category.toString()))
			return new EvaluationResult(getEmptyBag());
		
		AttributeValue AttResult = findAttributes(methodName, attributeId);

		return new EvaluationResult(AttResult);
	}

	private AttributeValue findAttributes(String methodName, URI attributeURI){
		List<AttributeValue> values = new ArrayList<AttributeValue>();
		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		query.add(new AttributeQuery(SubjectAttributeXmlName.ID,user.getId().toString(),StringAttribute.identifier));
	
		BagAttribute bag = null; 
		System.err.println("aNITA, LOOK HERE .");
		if(attributeURI.toString().equals(PermissionAttributeURI.RESOURCE_TYPE_URI))
		{
			System.err.println("Anita, the type is resource_type!!!");
			values.add(StringAttribute.getInstance("patientassignment"));
			try {
				bag = new BagAttribute(new URI(StringAttribute.identifier), values);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}else if(attributeURI.toString().equals(PermissionAttributeURI.ACTION_URI)){
			System.err.println("Anita, the type is action !!!");
			if(methodName.equals("savePatientAssignment")){
				System.err.println("Anita, the action is create and update !!!");
				values.add(StringAttribute.getInstance("create"));
				values.add(StringAttribute.getInstance("update"));
			}else 
				if(methodName.equals("deletePatientAssignment")){
					System.err.println("Anita, the action is delete !!!");
				values.add(StringAttribute.getInstance("delete"));
			}else 
				if(methodName.equals("searchPatientAssignment")){
					System.err.println("Anita, the action is view !!!");
				values.add(StringAttribute.getInstance("view"));
			}
			try {
				bag = new BagAttribute(new URI(StringAttribute.identifier), values);
			} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
			
		return bag;
	}
	
}
