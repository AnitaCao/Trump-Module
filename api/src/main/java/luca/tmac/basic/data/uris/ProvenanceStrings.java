package luca.tmac.basic.data.uris;

public class ProvenanceStrings {
	public static final String[] SETTER_METHOD_PREFIXES = {"create" ,"save","void","delete"};
	public static final String NS = "http://trump-india-uk.org/prov/";
	
	//activity namespace
	public static final String ACTIVITY_PATIENT = "save_patient/" ;
	public static final String ACTIVITY_PATIENT_ASSIGNMENT = "do_patientassignment/";
	public static final String ACTIVITY_UNDO_PATIENT_ASSIGNMENT = "undo_patientassignment/";
	
	// entity namespace
	public static final String ENTITY_PATIENT = "patient/";
	public static final String ENTITY_PATIENT_ASSIGNMENT = "patientassignment/";
	
	
	// agent namespace
	public static final String AGENT_USER = "user/";
	public static final String AGENT_PATIENT = "patient/";
	public static final String AGENT_DOCTOR = "doctor/";
	
	// property namespace
	public static final String PATIENT_NAME = "patient_name";
	public static final String DOCTOR_ID = "doctor_id";
	public static final String PATIENT_UUID = "patient_uuid";
	public static final String PATIENT_ASSIGNMENT_UUID = "patientassignment_uuid";
	public static final String ACTIVITY_NAME = "action_name";
	
	public static final String RDF ="http://www.w3.org/1999/02/22-rdf-syntax-ns#" ;
	public static final String OWL ="http://www.w3.org/2002/07/owl#" ;
	public static final String PROV = "http://www.w3.org/ns/prov#" ;
	public static final String XSD = "http://www.w3.org/2001/XMLSchema#" ;
	public static final String RDFs ="http://www.w3.org/2000/01/rdf-schema#" ;
	
	// query prefix 
	public static final String QUERY_PREFIX = 
			  " PREFIX agent: <" + ProvenanceStrings.NS + ProvenanceStrings.AGENT_USER + ">"
			+ " PREFIX pA: <" + ProvenanceStrings.NS + ProvenanceStrings.ENTITY_PATIENT_ASSIGNMENT + ">"
			+ " PREFIX p: <" + ProvenanceStrings.NS + ProvenanceStrings.ENTITY_PATIENT + ">" 
			+ " PREFIX RDF: <" + ProvenanceStrings.RDF + ">" 
			+ " PREFIX PROV: <" + ProvenanceStrings.PROV + ">" 
			+ " PREFIX NS: <" + ProvenanceStrings.NS + ">";
}
