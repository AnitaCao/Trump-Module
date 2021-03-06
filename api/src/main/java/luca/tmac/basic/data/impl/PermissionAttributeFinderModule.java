package luca.tmac.basic.data.impl;

import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import luca.data.DataHandler;
import luca.data.AttributeQuery;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.uris.PermissionAttributeURI;
import luca.tmac.basic.data.xml.PermissionAttributeXmlName;

public class PermissionAttributeFinderModule extends
		AbstractAttributeFinderModule {

	private URI defaultPermissionId;

	private Set<String> categories;
	private Set<String> ids;
	DataHandler data = null;

	public PermissionAttributeFinderModule(DataHandler pData) {

		try {
			defaultPermissionId = new URI(
					PermissionAttributeURI.PERMISSION_ID_URI);
		} catch (URISyntaxException e) {
		}

		// set Data Handler
		data = pData;

		// set supported categories
		categories = new HashSet<String>();
		categories.add(PermissionAttributeURI.PERMISSION_CATEGORY_URI);

		// set supported ids
		ids = new HashSet<String>();
		ids.add(PermissionAttributeURI.ACTION_URI);
		ids.add(PermissionAttributeURI.RESOURCE_ID_URI);
		ids.add(PermissionAttributeURI.RESOURCE_TYPE_URI);
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
		{
			return new EvaluationResult(getEmptyBag());
		}
		
		String id = getIdFromContext(defaultPermissionId, issuer,
				category, context);
		AttributeValue AttResult = findAttributes(id, attributeId);

		return new EvaluationResult(AttResult);
	}

	private AttributeValue findAttributes(String id, URI attributeURI) {
		String attribute = null;
		String attributeType = null;
		if (attributeURI.toString().equals(
				PermissionAttributeURI.RESOURCE_ID_URI)) {
			attributeType = StringAttribute.identifier;
			attribute = PermissionAttributeXmlName.RESOURCE_ID;
		}
		if (attributeURI.toString().equals(
				PermissionAttributeURI.RESOURCE_TYPE_URI)) {
			attributeType = StringAttribute.identifier;
			attribute = PermissionAttributeXmlName.RESOURCE_TYPE;
		}
		if (attributeURI.toString().equals(PermissionAttributeURI.ACTION_URI)) {
			attributeType = StringAttribute.identifier;
			attribute = PermissionAttributeXmlName.ACTION;
		}

		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		query.add(new AttributeQuery(PermissionAttributeXmlName.ID, id,StringAttribute.identifier));

		if (attribute != null)
			return data.getBagAttribute("permission", query, attribute,
					attributeType);

		// its not a permission but a resource attribute.. get resource id,type and
		// then attribute
		BagAttribute resourceIdBag = data.getBagAttribute("permission", query,
				PermissionAttributeURI.RESOURCE_ID_URI,
				StringAttribute.identifier);
		String resourceId = ((StringAttribute) resourceIdBag.getChildren().get(
				0)).getValue();

		BagAttribute resourceTypeBag = data.getBagAttribute("permission", query,
				PermissionAttributeURI.PERMISSION_ID_URI,
				StringAttribute.identifier);
		String resourceType = ((StringAttribute) resourceTypeBag.getChildren()
				.get(0)).getValue();
		
		query.clear();
		query.add(new AttributeQuery(PermissionAttributeXmlName.ID,resourceId,StringAttribute.identifier));
		return data.getBagAttribute(resourceType, query, attribute,
				attributeType);
	}
}
