package org.openmrs.module.trumpmodule;


import java.util.HashMap;

import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface TmacEnforceService extends OpenmrsService{
	public int isAuthorized(String priviledge, User user) throws APIException;
	
	//public List<String> getObligations() throws APIException;
	
	public int sendRequest(String privilege, User user);
	public HashMap<String,String> acceptResponse(String methodName);
}
