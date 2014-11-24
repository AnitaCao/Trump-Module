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
				String paUUID = ss[ss.length-1];
				System.out.println(paUUID);
				uuidList.add(paUUID);
			}
		}
		dataset.close();
		return uuidList;
	}
	
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
					}
				} else {
					System.out.println(cell.toString());
				}
			}
		}
		dataset.close();
		return properties;
	}
	
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
