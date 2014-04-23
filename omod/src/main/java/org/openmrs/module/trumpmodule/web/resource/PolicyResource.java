package org.openmrs.module.trumpmodule.web.resource;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
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


@Resource(name ="v1/trumpmodule/policy", supportedClass = Policy.class, supportedOpenmrsVersions = {"1.8.*", "1.9.*"})
public class PolicyResource extends DataDelegatingCrudResource<Policy> {

	private OpenmrsEnforceServiceContext OpenmrsContext = OpenmrsEnforceServiceContext.getInstance();
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("policyName");
			//description.addProperty("tags", Representation.REF);
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		}else if (rep instanceof FullRepresentation){
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("policyName");
			description.addProperty("content");
			description.addProperty("userId");
			
			//description.addProperty("tags", Representation.DEFAULT);
			description.addProperty("auditInfo", findMethod("getAuditInfo"));
			description.addSelfLink();
			return description;
		}
		return null;	
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addRequiredProperty("policyName");
		description.addRequiredProperty("content");

		return description;
	}
		
	@Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }
	

	@Override
	public Policy save(Policy delegate) {
		String personId = Context.getAuthenticatedUser().getId().toString();
		HashMap<String, ArrayList<Policy>> policies = OpenmrsContext.getPolicies();
		// set the policy person ID to be the authenticated user
		// this means that users can only upload policies belonging to themselves
		// which makes sense.
		delegate.setUserId(personId);
		policies.get(personId).add(delegate);
		OpenmrsContext.setPolicies(policies);
		
		// every time we save a policy, we should write them to disk
		// because we are not using a database for persistence.
		OpenmrsContext.savePolicies();
		
		return delegate;
	}

	@Override
	protected void delete(Policy delegate, String reason,
			RequestContext context) throws ResponseException {
		
		HashMap<String, ArrayList<Policy>> policies = OpenmrsContext.getPolicies();
		List<ArrayList<Policy>> allPolicies = new ArrayList<ArrayList<Policy>>(policies.values());
		for(ArrayList<Policy> ps : allPolicies){
			for(Policy p : ps){
				if(p.getId()==delegate.getId()){
					ps.remove(p);
					break;
				}
			}
		}
		OpenmrsContext.setPolicies(policies);
	}

	@Override
	public void purge(Policy delegate, RequestContext context)
			throws ResponseException {
		if(delegate == null)
			return;
	}

	@Override
	public Policy newDelegate() {
		return new Policy();
	}
	
	/**
	 * @param encounter
	 * @return encounter type and date
	 */
	public String getDisplayString(Policy policy) {
		return policy.getPolicyName();
	}

	@Override
	public Policy getByUniqueId(String uniqueId) {
		Policy policy = 
				OpenmrsContext.getPolicyByPolicyID(uniqueId);
		
		return policy;
	}

	@Override
	public NeedsPaging<Policy> doGetAll(RequestContext context){
		
		List<Policy> policyList = OpenmrsContext.getAllPolicies();
		return new NeedsPaging<Policy>(policyList, context);
	}



	
//	@Override
//	protected PageableResult doSearch(RequestContext context) {
//		String policyId = context.getRequest().getParameter("policy");
//		String query = context.getParameter("q");
//		if (policyId != null) {
//			
//			Policy policy = OpenmrsContext.getPolicyByPolicyID(policyId);
//			if (policy == null)
//				return new EmptySearchResult();
//			
//			List<Policy> po = new ArrayList<Policy>();
//			
//			po.add(OpenmrsContext.getPolicyByPolicyID(policyId));
//
//			return new NeedsPaging<Policy>(po, context);
//		}
//		return null;

		
//		return new ServiceSearcher<Policy>(EncounterService.class, "getEncounters", "getCountOfEncounters").search(
//		    context.getParameter("q"), context);
//	}
	


}
