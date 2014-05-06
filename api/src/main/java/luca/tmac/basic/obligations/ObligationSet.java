package luca.tmac.basic.obligations;
import java.util.List;

public class ObligationSet {
	
	//private DataHandler dh;
	//private String setId;
	private List<Obligation> list;
	private String decreasedBudget;
	
//	public ObligationSet(List<Obligation> oblList,DataHandler dh,int setId)
//	{
//		this.list = oblList;
//		this.dh = dh;
//		this.setId = setId;
//	}
//	
//	public ObligationSet(List<Obligation> obl,DataHandler dh)
//	{
//		
//		this.list = obl;
//		this.dh = dh;
//	//	setId = getLastSetId() + 1;
//
//	}
	
	public ObligationSet(List<Obligation> oblList,  String decreasedBudget){
		this.list = oblList;
//		this.setId = setId;
		this.decreasedBudget = decreasedBudget;
	
	}
	
//	public String getId(){
//		return setId;
//	}
	
	public boolean fulfilled(){		
		for(Obligation obl: list){
			if(!obl.isFulfilled())
				return false;
		}
		return true;
	}
	
	
//	public void add(Obligation obl)
//	{
//		list.add(obl);
//		AttributeQuery aq = new AttributeQuery(Obligation.SET_ID_ATTRIBUTE_NAME, Integer.toString(setId), "");
//		obl.setAttribute(aq);
//	}
	
	public List<Obligation> getList(){
		return list;
	}
	
	public void setList(List<Obligation> oblList){
		this.list = oblList;
	}

	public String getDecreasedBudget() {
		return decreasedBudget;
	}

	public void setDecreasedBudget(String decreasedBudget) {
		this.decreasedBudget = decreasedBudget;
	}
	
//	public int getLastSetId()
//	{
//		ArrayList<AttributeQuery> attributeQuery = new ArrayList<AttributeQuery>();
//		List<String> idList = dh.getAttribute("obligation", attributeQuery, Obligation.SET_ID_ATTRIBUTE_NAME);
//		int lastId = -1;
//		for(String id : idList)
//		{
//			int currentId = Integer.parseInt(id);
//			lastId = Math.max(lastId, currentId);
//		}
//		return lastId;
//	}
}
