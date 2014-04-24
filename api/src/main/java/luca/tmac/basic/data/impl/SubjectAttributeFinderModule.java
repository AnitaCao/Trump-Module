package luca.tmac.basic.data.impl;

import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.DateAttribute;
import org.wso2.balana.attr.DoubleAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import luca.data.DataHandler;
import luca.data.AttributeQuery;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.uris.SubjectAttributeURI;
import luca.tmac.basic.data.xml.SubjectAttributeXmlName;

public class SubjectAttributeFinderModule extends AbstractAttributeFinderModule {
	
	private URI defaultSubjectId;
	
	private Set<String> categories;
	private Set<String> ids;
	DataHandler data = null;
	
	public SubjectAttributeFinderModule(DataHandler pData) {
		
		try {
			defaultSubjectId = new URI(SubjectAttributeURI.ID_URI);
		} catch (URISyntaxException e) {
			// ignore
		}
		
		//set Data Handler
		data = pData;
		
		//set supported categories
		categories = new HashSet<String>();
		categories.add(SubjectAttributeURI.SUBJECT_CATEGORY_URI);
		
		//set supported ids
		ids = new HashSet<String>();
		ids.add(SubjectAttributeURI.ASSIGNED_PATIENT_URI);
		ids.add(SubjectAttributeURI.DOB_URI);
		ids.add(SubjectAttributeURI.FIRST_NAME_URI);
		ids.add(SubjectAttributeURI.LAST_NAME_URI);
		ids.add(SubjectAttributeURI.ROLE_URI);
		ids.add(SubjectAttributeURI.SUBJECT_CATEGORY_URI);
		ids.add(SubjectAttributeURI.TRUSTWORTHINESS_URI);
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
	
	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId,
			String issuer, URI category, EvaluationCtx context) {
		
		if(!getSupportedCategories().contains(category.toString()))
			return new EvaluationResult(getEmptyBag());
		
		String id = getIdFromContext(defaultSubjectId,issuer,category,context);
		AttributeValue AttResult = findAttributes(id, attributeId);

		return new EvaluationResult(AttResult);
	}

	private AttributeValue findAttributes(String id, URI attributeURI) {
		String attribute = null;
		String attributeType = null;
		
		if(attributeURI.toString().equals(SubjectAttributeURI.ROLE_URI))
		{
			attributeType = StringAttribute.identifier;
			attribute = SubjectAttributeXmlName.ROLE;
		}
		else if(attributeURI.toString().equals(SubjectAttributeURI.ASSIGNED_PATIENT_URI))
		{
			attributeType = StringAttribute.identifier;
			attribute = SubjectAttributeXmlName.ASSIGNED_PATIENT;
		}
		else if(attributeURI.toString().equals(SubjectAttributeURI.DOB_URI))
		{
			attributeType = DateAttribute.identifier;
			attribute = SubjectAttributeXmlName.DOB;
		}
		else if(attributeURI.toString().equals(SubjectAttributeURI.FIRST_NAME_URI))
		{
			attributeType = StringAttribute.identifier;
			attribute = SubjectAttributeXmlName.FIRST_NAME;
		}
		else if(attributeURI.toString().equals(SubjectAttributeURI.LAST_NAME_URI))
		{
			attributeType = StringAttribute.identifier;
			attribute = SubjectAttributeXmlName.LAST_NAME;
		}
		else if(attributeURI.toString().equals(SubjectAttributeURI.TRUSTWORTHINESS_URI))
		{
			attributeType = DoubleAttribute.identifier;
			attribute = SubjectAttributeXmlName.TRUSTWORTHINESS;
		}
		else if(attributeURI.toString().equals(SubjectAttributeURI.BUDGET_URI))
		{
			attributeType = DoubleAttribute.identifier;
			attribute = SubjectAttributeXmlName.BUDGET;
		}
		
		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		query.add(new AttributeQuery(SubjectAttributeXmlName.ID,id,StringAttribute.identifier));

		
		return data.getBagAttribute(SubjectAttributeXmlName.SUBJECT_TABLE, query, attribute, attributeType);
		
		 
		
	}
}
