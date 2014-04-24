package org.openmrs.module.trumpmodule.web.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationIds;
import luca.tmac.basic.obligations.ObligationImpl;

import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.obligations.RESTObligation;
import org.openmrs.module.trumpmodule.policies.Policy;
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
		Obligation ob = OpenmrsContext.getActiveObs().get(uniqueId);
		if(ob instanceof RESTObligation) 
			return (RESTObligation) ob;
		// if the obligation is some other type of implementation
		else if (ob instanceof ObligationImpl) {
			return new RESTObligation((ObligationImpl)ob);	
		}
		// dirty: but we shouldn't end up here.
		return null;
	}

	@Override
	protected void delete(RESTObligation delegate, String reason,
			RequestContext context) throws ResponseException {
		HashMap<String,Obligation> activeObs = OpenmrsContext.getActiveObs();
		
		activeObs.remove(delegate);
		OpenmrsContext.setActiveObs(activeObs);
	}

	@Override
	public void purge(RESTObligation delegate, RequestContext context)
			throws ResponseException {
		
	}
	@Override
	public RESTObligation newDelegate() {
		return new RESTObligation();
	}

	@Override
	public RESTObligation save(RESTObligation delegate) {
		
		HashMap<String,Obligation> activeObs = OpenmrsContext.getActiveObs();
		
		activeObs.put(delegate.getObUUID(),delegate);
		OpenmrsContext.setActiveObs(activeObs);
		return delegate;
	}
	
	@Override
	public NeedsPaging<Obligation> doGetAll(RequestContext context){
		
		List<Obligation> restObList = new ArrayList<Obligation>();
		HashMap<String, Obligation> activeObs = OpenmrsContext.getActiveObs();
		for(Entry<String, Obligation> e : activeObs.entrySet()){
			Obligation ob = e.getValue();
			if(ob.getActionName().equals(ObligationIds.REST_OBLIGATION_NAME_XML)){
				restObList.add(ob);
			} else if(ob instanceof ObligationImpl) {
				restObList.add(ob);
			}
		}
		return new NeedsPaging<Obligation>(restObList, context);
	}

	public String getDisplayString(RESTObligation rob) {
		return rob.getObligationDisplayString();
	}

}
