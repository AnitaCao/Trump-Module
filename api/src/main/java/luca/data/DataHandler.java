package luca.data;

import java.util.List;

import org.wso2.balana.attr.BagAttribute;

/**
 * Retrieve data from a data source
 * */
public interface DataHandler {
	
	/** Retrieve an attribute from the instance of entity with specified pairs name-value
	 * @param entity
	 * @param attributeQuery list of pairs name-value to query the istances
	 * @param attributeName name of the attribute to retrieve
	 * @param attributeType the URI of the attributeType
	 * @return a bag containing the values for the specified attributes
	 * */
	public abstract BagAttribute getBagAttribute(String entity, List<AttributeQuery> attributeQuery,
			String attributeName, String attributeType);
	
	/** Retrieve an attribute from the instance of entity with specified pairs name-value
	 * @param entity
	 * @param attributeQuery list of pairs name-value to query the istances
	 * @param attributeName name of the attribute to retrieve
	 * @return a list of string containing the values for the specified attributes
	 * */
	public List<String> getAttribute(String entity, List<AttributeQuery> attributeQuery,
			String attributeName);
	
	public String write(String entity, List<AttributeQuery> attributes);
	
	public boolean contains(String entity, String id);

	boolean remove(String entity, String id);
	
	boolean modifyAttribute(String entity,String id,List<AttributeQuery> attributes);

	boolean addAttribute(String entity, String id,
			List<AttributeQuery> attributes);
	
	public List<AttributeQuery> getAttributesOf(String entity,String id);
	
	public String getNextId(String entity);
}
