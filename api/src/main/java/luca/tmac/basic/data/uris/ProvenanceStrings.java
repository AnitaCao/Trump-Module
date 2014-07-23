package luca.tmac.basic.data.uris;

public class ProvenanceStrings {
	public static final String[] SETTER_METHOD_PREFIXES = {"create" ,"save","void","delete"};
	public static final String NS = "http://trump-india-uk.org/prov/";
	
	//activity namespace
	public static final String ACTIVITY_NS = "action_name";
	
	// entity namespace
	public static final String ENTITY_PATIENT = "entity/patient/";
	public static final String ENTITY_PATIENT_ASSIGNMENT = "entity/patientassignment/";
	
	
	// agent namespace
	public static final String AGENT_USER = "agent/user";
	public static final String AGENT_PATIENT = "agent/patient";
	public static final String AGENT_DOCTOR = "agent/doctor";
	
	// property namespace
	public static final String PATIENT_NAME = "patient_name";
	public static final String DOCTOR_ID = "doctor_id";
	public static final String PATIENT_UUID = "patient_uuid";
	
	public static final String RDF ="http://www.w3.org/1999/02/22-rdf-syntax-ns#" ;
	public static final String OWL ="http://www.w3.org/2002/07/owl#" ;
	public static final String PROV = "http://www.w3.org/ns/prov#" ;
	public static final String XSD = "http://www.w3.org/2001/XMLSchema#" ;
	public static final String RDFs ="http://www.w3.org/2000/01/rdf-schema#" ;
}
