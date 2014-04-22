package org.openmrs.module.trumpmodule.web.resource;

import java.util.ArrayList;
import java.util.List;


import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.obligations.RESTObligation;
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

@Resource(name ="v1/trumpmodule/restobligation", supportedClass = RESTObligation.class, supportedOpenmrsVersions = {"1.8.*", "1.9.*"})
public class RESTObligationResource extends DataDelegatingCrudResource<RESTObligation> {
	
	private OpenmrsEnforceServiceContext OpenmrsContext = OpenmrsEnforceServiceContext.getInstance();

	@Override
	public DelegatingResourceDescription getRepresentationDescription(
			Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("actionName");
			description.addProperty("id");
			description.addProperty("startDate");
			//description.addProperty("tags", Representation.REF);
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		}else if (rep instanceof FullRepresentation){
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("actionName");
			description.addProperty("attributeMap");
			description.addProperty("id");
			description.addProperty("startDate");
			
			//description.addProperty("tags", Representation.DEFAULT);
			description.addProperty("auditInfo", findMethod("getAuditInfo"));
			description.addSelfLink();
			return description;
		}
		return null;	
	}

	@Override
	public RESTObligation getByUniqueId(String uniqueId) {
		return null;
	}

	@Override
	protected void delete(RESTObligation delegate, String reason,
			RequestContext context) throws ResponseException {
		
	}

	@Override
	public void purge(RESTObligation delegate, RequestContext context)
			throws ResponseException {
		
	}
	@Override
	public RESTObligation newDelegate() {
		return null;
	}

	@Override
	public RESTObligation save(RESTObligation delegate) {
		return null;
	}
	
	@Override
	public NeedsPaging<RESTObligation> doGetAll(RequestContext context){
		
//		HashMap<UUID, UserObRelation> activeObs = OpenmrsContext.getActiveObs();
		List<RESTObligation> restObList = new ArrayList<RESTObligation>();
//		for(UserObRelation uo : activeObs.values()){
//			if(uo.obligation.getActionName().equals(ObligationIds.REST_OBLIGATION_NAME_XML)){
//				restObList.add((RESTObligation) uo.obligation);
//			}
//		}
		
		return new NeedsPaging<RESTObligation>(restObList, context);
	}


}
