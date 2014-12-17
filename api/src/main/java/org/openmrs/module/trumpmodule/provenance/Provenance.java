/**
 * This class is a control class for searching information in dataset (TDB)
 * it contains TDB queries.
 */
package org.openmrs.module.trumpmodule.provenance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.openmrs.module.trumpmodule.OpenmrsEnforceServiceContext;

import luca.tmac.basic.data.uris.ProvenanceStrings;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.tdb.TDBFactory;

public class Provenance {	
	private Dataset dataset;
	OpenmrsEnforceServiceContext openmrsContext = OpenmrsEnforceServiceContext
			.getInstance();
	private String directory = openmrsContext.getProvenanceDirectory();
	
	
	/**
	 * get all objects which is the given dataType, for example, if the dataType is "PatientAssignment",
	 * this method will get all the patientAssignment objects' uuids
	 * @param dataType the required dataType, such as "PatientAssignment", "Concept","Policy"..
	 * @return uuidList the objects' uuids
	 */
	public List<String> getAll(String dataType) {
		List<String> uuidList = new ArrayList<String>();

		String q = ProvenanceStrings.getQueryPerfix(dataType) + "SELECT ?s " + "WHERE {"+ "?s a PROV:Entity .}";

		dataset = TDBFactory.createDataset(directory);
		Query query = QueryFactory.create(q);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		ResultSet results = qexec.execSelect();
		
		while (results.hasNext()) {
			QuerySolution row = results.next();
			String things = row.get("s").toString();
			if (things.contains(dataType)) {
				String[] ss = things.split("/");
				//this uuid is from the url of the resource in TDB
				String UUID = ss[ss.length-1];
				System.out.println(UUID);
				uuidList.add(UUID);
			}
		}
		dataset.close();
		return uuidList;
	}
	
	/**
	 * Get the object by given dataType and the uuid of the required object
	 * @param dataType  the required dataType, such as "PatientAssignment", "Concept","Policy"..
	 * @param uuid  the uuid of the required object
	 * @return properties  the required object's property hashMap, which contain the properties 
	 * from TDB, we will use this hashMap to create a object. (information in TDB is not object.)
	 */
	public HashMap<String,String> getByUUID(String dataType,String uuid){
		HashMap<String,String> properties = new HashMap<String, String>();
		
		String q = ProvenanceStrings.getQueryPerfix(dataType) + "SELECT *  WHERE {"
				+ "entity:" +uuid + " ?property ?value}";

		System.err.println(q);
		dataset = TDBFactory.createDataset(directory);
		Query query = QueryFactory.create(q);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		ResultSet results = qexec.execSelect();
		while (results.hasNext()) {

			QuerySolution row = results.next();
			Iterator columns = row.varNames();

			while (columns.hasNext()) {
				RDFNode cell = row.get((String) columns.next());
				if (cell.isResource()) {
					com.hp.hpl.jena.rdf.model.Resource resource = cell
							.asResource();
					String resourceString = resource.toString();
					//if the resource string contains the properties' namespace, which means this resource is actually
					//a property of an Entity (we only put entity properties when we store information to TDB)
					if (resourceString.contains(ProvenanceStrings.NS)) {
						properties.put(resourceString.replace(ProvenanceStrings.NS, ""), row.get((String) columns.next()).toString());

					} else if (resourceString.contains("wasInvalidatedBy")){
						properties.put("invalidated","true");
					} else {
						//if the property name does not contain the properties we want to get, we need to 
						//jump two to skip the next value of the property which we don't need ( actually, 
						//we are jumping to next row by skipping the next value.)
						columns.next();
					}
				} else {
					System.out.println(cell.toString());
				}
			}
		}
		dataset.close();
		return properties;
	}
	
	/**
	 * Check if the required object is exist in TDB or not
	 * @param dataType  the dataType of the object
	 * @param propeties  the properties of the object
	 * @return
	 */
	public boolean checkExist(String dataType, HashMap<String,String> propeties){
		
		boolean exist = false;
		boolean isInValidated = false;
		
		StringBuilder tmp = new StringBuilder();
		tmp.append(ProvenanceStrings.getQueryPerfix(dataType) + "SELECT *  WHERE { ");
		for(String key : propeties.keySet()){
			tmp.append("?pa NS:" + key+ "'" + propeties.get(key)+"'.");
		}
		tmp.append("?pa PROV:wasGeneratedBy ?assign_activity ."
        		+ "?assign_activity PROV:startedAtTime ?assign_time ."
        		+ "OPTIONAL { ?pa PROV:wasInvalidatedBy ?unassign_activity ."
        		+            "?unassign_activity PROV:startedAtTime ?unassign_time .}"
        		+ "}");
		dataset = TDBFactory.createDataset(directory);
		Query query = QueryFactory.create(tmp.toString());
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		ResultSet results = qexec.execSelect();
		while(results.hasNext()){
			exist = true;
			QuerySolution row = results.next();
			RDFNode unassignTimeNode = row.get("unassign_time");
			if(unassignTimeNode!=null){
				String unassignTime = unassignTimeNode.toString();
				String assignTime = row.get("assign_time").toString();
				System.err.println("the unassign_time is : " + unassignTime);
				System.err.println("the assign_time is : " + assignTime);
				System.out.println(unassignTime.compareTo(assignTime));
				if(unassignTime.compareTo(assignTime)>=0){ //means unassign_activity happened at a later time
					                                       //which means it has been unassigned.
					isInValidated = true;
				}
				
			}else isInValidated = false; //if there is no unassignTimeNode, which means this activity has not been invalidated.
		}
		return (exist&&!isInValidated);
	}
	
}
