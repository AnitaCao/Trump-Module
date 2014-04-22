/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.trumpmodule;


import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.aop.AuthorizationAdvice;
import org.openmrs.api.context.Context;
import org.openmrs.module.Activator;
import org.openmrs.api.OpenmrsService;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class TrumpModuleActivator implements Activator {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * @see org.openmrs.module.Activator#startup()
	 */
	public void startup() {
		log.info("Starting Advice Killer Module");
		System.out.println("Bye bye authorization advice!1");
		reInitialServiceAdvices();
		OpenmrsEnforceServiceContext SerContext = OpenmrsEnforceServiceContext.getInstance();
		SerContext.getActiveObs().clear();
		SerContext.getExpiredObs().clear();
		SerContext.getFulfilledObs().clear();

	
	}
	
	/**
	 * @see org.openmrs.module.Activator#shutdown()
	 */
	public void shutdown() {
		log.info("Shutting down Trump Module");
		
		// here, we need to write all the policy objects from the context back to the
		// local policy directory as files.
	}
	
	/**
	 * This method will remove AuthorizationAdvice from a particular core OpenMRS service.
	 */
	public void reInitialServiceAdvices(){

		ArrayList<OpenmrsService> services = new ArrayList<OpenmrsService>();
		
		// painstakingly add all the services to a list, because reflection is dangerous
		// not the tidiest way, but hey...
		services.add(Context.getPatientService());
		services.add(Context.getAdministrationService());
		services.add(Context.getAlertService());
		services.add(Context.getCohortService());
		services.add(Context.getConceptService());
		services.add(Context.getEncounterService());
		services.add(Context.getFormService());
		services.add(Context.getHL7Service());
		services.add(Context.getLocationService());
		services.add(Context.getObsService());
		services.add(Context.getOrderService());
		services.add(Context.getPatientService());
		services.add(Context.getProgramWorkflowService());
		services.add(Context.getReportObjectService());
		services.add(Context.getSchedulerService());
		services.add(Context.getSerializationService());

		for(OpenmrsService service : services) {
			
			Advised advisedService = (Advised) service;
			Advisor[] advisors = advisedService.getAdvisors();
			for(Advisor a : advisors) {
				if(a.getAdvice() instanceof AuthorizationAdvice)  {
					advisedService.removeAdvice(a.getAdvice());	
				}
			}
		}
	}
		

}
