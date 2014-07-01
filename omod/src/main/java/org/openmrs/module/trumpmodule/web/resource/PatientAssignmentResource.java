package org.openmrs.module.trumpmodule.web.resource;



import java.util.Arrays;
import java.util.List;

import org.openmrs.module.trumpmodule.patientassignment.PatientAssignment;
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


@Resource(name ="v1/trumpmodule/policy", supportedClass = PatientAssignment.class, supportedOpenmrsVersions = {"1.8.*", "1.9.*"})
public class PatientAssignmentResource extends DataDelegatingCrudResource<PatientAssignment> {

	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("patientName");
			description.addProperty("doctorId");
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		}else if (rep instanceof FullRepresentation){
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("patientName");
			description.addProperty("doctorId");
			description.addProperty("patientUUID");
			description.addProperty("userId");
			
			description.addProperty("auditInfo", findMethod("getAuditInfo"));
			description.addSelfLink();
			return description;
		}
		return null;	
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addRequiredProperty("doctorId");

		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addRequiredProperty("doctorId");

		return description;
	}
	
	@Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT);
    }
	

	@Override
	public PatientAssignment save(PatientAssignment delegate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void delete(PatientAssignment delegate, String reason,
			RequestContext context) throws ResponseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void purge(PatientAssignment delegate, RequestContext context)
			throws ResponseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PatientAssignment newDelegate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PatientAssignment getByUniqueId(String uniqueId) {
		// TODO get from TDB
		return null;
	}

	@Override
	public NeedsPaging<Policy> doGetAll(RequestContext context){
		// TODO get from TDB
		return null;
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
//
//		
//		return new ServiceSearcher<Policy>(EncounterService.class, "getEncounters", "getCountOfEncounters").search(
//		    context.getParameter("q"), context);
//	}
	


}
