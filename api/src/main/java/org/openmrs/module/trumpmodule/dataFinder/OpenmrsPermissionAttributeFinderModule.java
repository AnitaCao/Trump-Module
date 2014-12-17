package org.openmrs.module.trumpmodule.dataFinder;


import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.xacml3.Attributes;

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
import luca.tmac.basic.data.uris.SubjectAttributeURI;
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
	
		System.out.println("Anita message for you: role=" + user.getAllRoles() + " userID : " + user.getId());
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
		
		Set<Attributes> attributesSet = context.getRequestCtx().getAttributesSet();
		//get the required permission from the request, which actually is the privilege from the annotation.
		Set<Attribute> permissionAttribute = null;
		
		for(Attributes atr : attributesSet){
			if(atr.getCategory().toString().equalsIgnoreCase(PermissionAttributeURI.PERMISSION_CATEGORY_URI)){
				permissionAttribute = atr.getAttributes();
				break;
			}
		}
		
		AttributeValue AttResult = findAttributes(methodName, attributeId,permissionAttribute);

		return new EvaluationResult(AttResult);
	}

	private AttributeValue findAttributes(String methodName, URI attributeURI, Set<Attribute> attrSet){
		List<AttributeValue> values = new ArrayList<AttributeValue>();
		String actionString = null;
    	String resourceString = null;
    	if(!attrSet.isEmpty()){
			for(Attribute a:attrSet){
				String permission = ((StringAttribute)(a.getValues().get(0))).getValue();
				String[] parms = permission.split(" ", 2);
				actionString = parms[0];
				resourceString = parms[1];
			}
		}
		
		BagAttribute bag = null; 
		
		if(methodName.contains("PatientAssignment")){
			if(attributeURI.toString().equals(PermissionAttributeURI.RESOURCE_TYPE_URI))
			{
				values.add(StringAttribute.getInstance("patientassignment"));
				
			}else if(attributeURI.toString().equals(PermissionAttributeURI.ACTION_URI)){
				if(methodName.equals("savePatientAssignment")){
					values.add(StringAttribute.getInstance("create"));
					values.add(StringAttribute.getInstance("update"));
				}else 
					if(methodName.equals("deletePatientAssignment")){
					values.add(StringAttribute.getInstance("delete"));
				}else 
					if(methodName.equals("searchPatientAssignment")){
					values.add(StringAttribute.getInstance("view"));
				}
			}
				
		}
		// if the method is not about patientassignment, which means it is something from openmrs,
		// then we use the permission attributes we got from openmrs annotation. 
		else {
			if(attributeURI.toString().equals(PermissionAttributeURI.RESOURCE_TYPE_URI)){
				values.add(StringAttribute.getInstance(resourceString));
			}else if(attributeURI.toString().equals(PermissionAttributeURI.ACTION_URI)){
				values.add(StringAttribute.getInstance(actionString));
			}else if(attributeURI.toString().equals(PermissionAttributeURI.WANTED_PATIENT_URI))
			{	
				values.add(StringAttribute.getInstance(parameters[0].toString()));
				try {
					bag = new BagAttribute(new URI(StringAttribute.identifier), values);
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}	
			}
		}
		
		try {
			bag = new BagAttribute(new URI(StringAttribute.identifier), values);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
			
		return bag;
	}
	
}
