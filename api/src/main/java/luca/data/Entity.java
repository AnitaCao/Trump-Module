package luca.data;

import java.util.List;
import java.util.HashMap;

public class Entity {
	public HashMap<String, String> attributeMap;
	
	public Entity(List<AttributeQuery> list)
	{
		attributeMap = new HashMap<String,String>();
		for(AttributeQuery aq : list)
		{
			attributeMap.put(aq.name,aq.value);
		}
	}
	
	public String getAttribute(String name)
	{
		return attributeMap.get(name);
	}
}
