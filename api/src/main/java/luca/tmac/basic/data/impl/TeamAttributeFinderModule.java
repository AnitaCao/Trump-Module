package luca.tmac.basic.data.impl;

import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.DoubleAttribute;
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
import luca.tmac.basic.data.uris.TeamAttributeURI;
import luca.tmac.basic.data.xml.TeamAttributeXmlName;

public class TeamAttributeFinderModule extends AbstractAttributeFinderModule {

	private URI defaultTeamId;

	private Set<String> categories;
	private Set<String> ids;
	DataHandler data = null;

	public TeamAttributeFinderModule(DataHandler pData) {

		try {
			defaultTeamId = new URI(TeamAttributeURI.ID_URI);
		} catch (URISyntaxException e) {
			// ignore
		}

		// set Data Handler
		data = pData;

		// set supported categories
		categories = new HashSet<String>();
		categories.add(TeamAttributeURI.TEAM_CATEGORY_URI);

		// set supported ids
		ids = new HashSet<String>();
		//ids.add(TeamAttributeURI.ASSIGNED_TASK_URI);
		ids.add(TeamAttributeURI.MEMBER_ID_URI);
		ids.add(TeamAttributeURI.BUDGET_URI);
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

		if (!getSupportedCategories().contains(category.toString()))
			return new EvaluationResult(getEmptyBag());

		String id = getIdFromContext(defaultTeamId, issuer, category, context);
		AttributeValue AttResult = findAttributes(id, attributeId);

		return new EvaluationResult(AttResult);
	}

	private AttributeValue findAttributes(String id, URI attributeURI) {
		String attribute = null;
		String attributeType = null;

		/*
		 * if
		 * (attributeURI.toString().equals(TeamAttributeURI.ASSIGNED_TASK_URI))
		 * {
		 * 
		 * attributeType = StringAttribute.identifier; attribute =
		 * TeamAttributeXmlName.ASSIGNED_TASK; }
		 */
		if (attributeURI.toString().equals(TeamAttributeURI.MEMBER_ID_URI)) {
			attributeType = StringAttribute.identifier;
			attribute = TeamAttributeXmlName.MEMBER_ID;
		}
		else if(attributeURI.toString().equals(TeamAttributeURI.BUDGET_URI))
		{
			attributeType = DoubleAttribute.identifier;
			attribute = TeamAttributeXmlName.BUDGET;
		}

		ArrayList<AttributeQuery> query = new ArrayList<AttributeQuery>();
		query.add(new AttributeQuery(TeamAttributeXmlName.ID, id,
				StringAttribute.identifier));

		return data.getBagAttribute(TeamAttributeXmlName.TEAM_TABLE, query,
				attribute, attributeType);
	}
}
