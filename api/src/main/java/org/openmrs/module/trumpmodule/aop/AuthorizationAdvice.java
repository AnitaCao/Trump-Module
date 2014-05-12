package org.openmrs.module.trumpmodule.aop;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.annotation.AuthorizedAnnotationAttributes;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;
import org.openmrs.module.trumpmodule.authorization.TmacEnforceServiceImpl;
import org.springframework.aop.MethodBeforeAdvice;


public class AuthorizationAdvice implements MethodBeforeAdvice {
	
	
	
	/**
	 * Logger for this class and subclasses
	 */
	protected final Log log = LogFactory.getLog(AuthorizationAdvice.class);
	OpenmrsEnforceServiceContext SerContext = OpenmrsEnforceServiceContext.getInstance();
	
	/**
	 * Allows us to check whether a user is authorized to access a particular method.
	 * 
	 * @param method
	 * @param args
	 * @param target
	 * @throws Throwable
	 * @should notify listeners about checked privileges
	 */
	@SuppressWarnings( { "unchecked" })
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (log.isDebugEnabled()) {
			log.debug("Calling authorization advice before " + method.getName());
		}
		System.err.println("Calling authorization advice before " + method.getName());
		
		//get current user 
		User user = Context.getAuthenticatedUser();
		
		if (log.isDebugEnabled()) {
			log.debug("User " + user);
			if (user != null) {
				log.debug("has roles " + user.getAllRoles());
			}
		}
		
		AuthorizedAnnotationAttributes attributes = new AuthorizedAnnotationAttributes();
		Collection<String> privileges = attributes.getAttributes(method);  //get the strings such as "view patient", which is a combination of action and resource in Luca's code
		
		boolean requireAll = attributes.getRequireAll(method);
		System.out.println("!!!!!requireAll: " + requireAll);
		if (!privileges.isEmpty()) {		
			
			
			//JUST FOR TESTING: we need to hack the before method here for our method to get through without checking privileges 
			if(method.getName().equalsIgnoreCase("getpatients") && args.length>3){
				return ;
			}
			
			
			//TmacEnforceServiceImpl pepService = Context.getService(TmacEnforceServiceImpl.class);
			TmacEnforceServiceImpl pepService = new TmacEnforceServiceImpl(args,method.getName());
			
			
			for (String privilege : privileges) {
				
				if (privilege == null || privilege.isEmpty())
					return;
				
			    if(pepService.isAuthorized(privilege, user)){
			    	
			    	//get obligation and perform system obligation, system obligation will be performed automatically
			    	HashMap<String,String> messages = pepService.acceptResponse(method.getName()); 
			    	
			    	for(String ss : messages.keySet()){
				    	System.err.println("Anita! Obligations : "+ss + " ," +messages.get(ss));
			    	}
			    	
			    	System.out.println("Anita ! the size of the active obligation is : " + SerContext.getActiveObs().size());
			    	
			    	System.out.println(user.getUsername() + "Anita !!!!!!! is Authorized !!!");
			    	
			    	if(!requireAll){ 
			    		return; 
			    		}
			    }
			    else {
					if (requireAll) {
						// if all are required, the first miss causes them
						// to "fail"
						throwUnauthorized(user, method, privilege);
					}
			    }
			}
			if (requireAll == false) {
				// If there's no match, then we know there are privileges and
				// that the user didn't have any of them. The user is not
				// authorized to access the method
				System.out.println("1 Anita. Calling me !");
				throwUnauthorized(user, method, privileges);
				
			}
		}
		else if (attributes.hasAuthorizedAnnotation(method)) {
			
			// if there are no privileges defined, just require that 
			// the user be authenticated
			if (Context.isAuthenticated() == false)
				throwUnauthorized(user, method);
		}		
		
	}
	
	/**
	 * Throws an APIAuthorization exception stating why the user failed
	 * 
	 * @param user authenticated user
	 * @param method acting method
	 * @param attrs Collection of String privilege names that the user must have
	 */
	private void throwUnauthorized(User user, Method method, Collection<String> attrs) {
		if (log.isDebugEnabled())
			log.debug("User " + user + " is not authorized to access " + method.getName());
		throw new APIAuthenticationException("Privileges required: " + attrs+ " Sorry! User : " + user.getAllRoles()+" "+user.getUsername()+ " do not have the privilege!");
	}
	
	/**
	 * Throws an APIAuthorization exception stating why the user failed
	 * 
	 * @param user authenticated user
	 * @param method acting method
	 * @param attrs privilege names that the user must have
	 */
	private void throwUnauthorized(User user, Method method, String attr) {
		if (log.isDebugEnabled())
			log.debug("User " + user + " is not authorized to access " + method.getName());
		throw new APIAuthenticationException("Privilege required: " + attr + " Sorry! User : " + user.getName()+ " do not have the privilege!");
	}
	
	/**
	 * Throws an APIAuthorization exception stating why the user failed
	 * 
	 * @param user authenticated user
	 * @param method acting method
	 */
	private void throwUnauthorized(User user, Method method) {
		if (log.isDebugEnabled())
			log.debug("User " + user + " is not authorized to access " + method.getName());
		throw new APIAuthenticationException("Basic authentication required");
	}
	

}

