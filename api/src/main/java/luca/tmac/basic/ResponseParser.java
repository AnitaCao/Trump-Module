package luca.tmac.basic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.data.XmlDataHandler;
import luca.tmac.basic.obligations.ObligationImpl;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationSet;

public class ResponseParser {
	
	public static final String DENY_RESPONSE = "Deny";
	public static final String PERMIT_RESPONSE = "Permit";
	public static long incremental_id= 0;
	
	
	private long parser_id;
	private String xacmlResponse;
	private String decision;
	private ObligationSet oblSet;
	
	public ResponseParser(String response,DataHandler dh)
	{
		parser_id = incremental_id++;
		xacmlResponse = response;
		List<Obligation> obl = getObligationsFromResponse(xacmlResponse);
		oblSet = new ObligationSet(obl,dh);
		decision = parseDecision(xacmlResponse);
	}
	
	public long getParserId()
	{
		return parser_id;
	}
	
	
	public String getDecision()
	{
		return decision;
	}
	
	public ObligationSet getObligation()
	{
		return oblSet;
	}
	
	private String parseDecision(String response)
	{
		String xPathSelector = "//Response/Result/Decision/text()";
		NodeList decisionNodeList = XmlDataHandler.getNodeListFromXpathString(response, xPathSelector);
		if(decisionNodeList.getLength() == 0)
			return null;
		Node n = decisionNodeList.item(0);
		return n.getTextContent();
	}
	
	
	public static List<Obligation> getObligationsFromResponse(String response) {

		String xPathSelector = "//Obligations/Obligation";
		NodeList obligationNodeList = XmlDataHandler
				.getNodeListFromXpathString(response, xPathSelector);
		List<Obligation> sysOblList = new ArrayList<Obligation>();

		
		for (int i = 0; i < obligationNodeList.getLength(); i++) {
			Node node = obligationNodeList.item(i);
			sysOblList.add(getObligationInstanceFromResponseNode(node));
		}
		return sysOblList;
	}

	public static Obligation getObligationInstanceFromResponseNode(Node obligationNode) {
		NamedNodeMap att = obligationNode.getAttributes();
		Node idNode = att.getNamedItem("ObligationId");
		if (idNode == null)
			return null;
		String obligationId = idNode.getNodeValue();

		ArrayList<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
		NodeList nList = obligationNode.getChildNodes();

		for (int i = 0; i < nList.getLength(); i++) {

			Node n = nList.item(i);
			if (n.getNodeName().equals("AttributeAssignment")) {
				String attributeValue = n.getTextContent();

				NamedNodeMap childAtt = n.getAttributes();
				String attributeId = childAtt.getNamedItem("AttributeId")
						.getNodeValue();

				String attributeType = childAtt.getNamedItem("DataType")
						.getNodeValue();

				attributes.add(new AttributeQuery(attributeId, attributeValue,
						attributeType));
			}
		}
		return new ObligationImpl(obligationId, null, new Date(), attributes);
	}
	
}
