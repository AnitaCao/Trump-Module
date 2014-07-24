package org.openmrs.module.trumpmodule.patientassignment;


import java.io.Serializable;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.api.context.Context;

public class PatientAssignment extends BaseOpenmrsData implements Serializable {
    
     private static final long serialVersionUID = 1L;
     private Integer id;
     private String patientUUID;
     private String patientName;
     private String doctorId; //assigned to this doctor, actually this doctorId is an userId
     private String userId; //who did this assignment
     private String patientassignmentUUID;
    
     public PatientAssignment(){
          // TODO - fix this - super dirty generation of a not very unique integer ID just to keep
          // OpenMRS happy for now. But we will need a better solution in production. It seems that
          // openmrs objects get their integer IDs after serialisation in the database (a bit like
          // Ruby on Rails ) and since we are not using the database, we don't get this. But we are
          // only going to use UUID anyway so it shouldn't be a problem.
          this.id = new Long(System.currentTimeMillis() / 1000L).intValue();
          this.userId = Context.getAuthenticatedUser().getId().toString();
     }
    
     public PatientAssignment(String patientUUID, String doctorId) {
          this.patientUUID = patientUUID;
         this.doctorId = doctorId;

     }
    
     public String getUserId() {
          return userId;
     }

     public void setUserId(String userId) {
          this.userId = userId;
     }


     public String getPatientName() {
          return patientName;
     }

     public void setPatientName(String patientName) {
          this.patientName = patientName;
     }

     public Integer getId() {
          // this is bad - but to be an OpenMRSData it needs to
          return (int) this.id;
     }

     public void setId(Integer newId) {
          this.id = newId;
         
     }

     public String getPatientUUID() {
          return patientUUID;
     }

     public void setPatientUUID(String patientUUID) {
          this.patientUUID = patientUUID;
     }

     public String getDoctorId() {
          return doctorId;
     }

     public void setDoctorId(String doctorId) {
          this.doctorId = doctorId;
     }

	public String getPatientassignmentUUID() {
		return this.getUuid();
	}

	public void setPatientassignmentUUID(String patientassignmentUUID) {
		this.setUuid(patientassignmentUUID);
	}
    
}
