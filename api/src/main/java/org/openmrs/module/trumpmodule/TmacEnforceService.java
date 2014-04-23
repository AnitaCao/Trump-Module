package org.openmrs.module.trumpmodule;


import org.openmrs.User;
import org.openmrs.api.APIException;

public interface TmacEnforceService {
	public boolean isAuthorized(String priviledge, User user) throws APIException;
	
	//public List<String> getObligations() throws APIException;

}
