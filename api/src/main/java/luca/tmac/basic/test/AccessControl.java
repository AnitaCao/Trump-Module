package luca.tmac.basic.test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.Balana;
import org.wso2.balana.ParsingException;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.AttributeAssignment;
import org.wso2.balana.ctx.ResponseCtx;
import org.wso2.balana.xacml3.Advice;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import luca.data.AttributeQuery;
import luca.data.XmlDataHandler;
import luca.tmac.basic.ResponseParser;
import luca.tmac.basic.TmacPDP;
import luca.tmac.basic.data.uris.ActionAttributeURI;
import luca.tmac.basic.data.xml.PermissionAttributeXmlName;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationImpl;
import luca.tmac.basic.obligations.ObligationImpl;
import luca.tmac.basic.obligations.UserObligationMonitor;

public class AccessControl {

	@SuppressWarnings("unused")
	private static Balana balana;
	public static final String RESOURCE_PATH = "./tmac/basic/resources/data.xml";
	public static final String TOP_LEVEL_POLICIES_PATH = "./tmac/basic/top-level-policies";
	public static final String OTHER_POLICIES_PATH = "./tmac/basic/other-policies";

	public static void main(String[] args) {

		Scanner console = new Scanner(System.in);

		TmacPDP pdp = new TmacPDP(new XmlDataHandler(RESOURCE_PATH), TOP_LEVEL_POLICIES_PATH,
				OTHER_POLICIES_PATH);

		System.out.println("Insert your id: ");
		String user_id = console.next();

		System.out
				.println("choose:\n1. Assign task to team\n2. Obtain permission");
		int choice = console.nextInt();
		String action = null;
		if (choice == 1)
			action = ActionAttributeURI.ACTION_TASK_ASSIGNMENT;
		else
			action = ActionAttributeURI.ACTION_OBTAIN_PERMISSION;
		XmlDataHandler xd = new XmlDataHandler(RESOURCE_PATH);
		
		List<String> permission_ids = new ArrayList<String>();
		if (choice != 1) {
			System.out.println("Insert the patient id");
			String patient_id = console.next();

			System.out.println("Choose one action:");
			String permission_action = console.next();

			
			ArrayList<AttributeQuery> attributeQuery = new ArrayList<AttributeQuery>();

			attributeQuery.add(new AttributeQuery(
					PermissionAttributeXmlName.ACTION, permission_action,
					StringAttribute.identifier));
			attributeQuery.add(new AttributeQuery(
					PermissionAttributeXmlName.RESOURCE_ID, patient_id,
					StringAttribute.identifier));
			attributeQuery.add(new AttributeQuery(
					PermissionAttributeXmlName.RESOURCE_TYPE, "user",
					StringAttribute.identifier));

			permission_ids = xd.getAttribute(
					PermissionAttributeXmlName.PERMISSION_TABLE,
					attributeQuery, PermissionAttributeXmlName.ID);
		}
		
		if (permission_ids.size() == 0) {
			permission_ids.add("");
		}
		
		System.out.println("team id:");
		String team_id = console.next();

		System.out.println("task id:");
		String task_id = console.next();

		String request = "";
				//pdp.createXACMLRequest(user_id, permission_ids.get(0),team_id, task_id, action);

		System.out
				.println("\n======================== XACML Request ====================");
		System.out.println(request);
		System.out
				.println("===========================================================");

		String response = pdp.evaluate(request);
		

		System.out
				.println("\n======================== XACML Response ===================");
		System.out.println(response);
		System.out
				.println("===========================================================");

		try {
			ResponseCtx responseCtx = ResponseCtx
					.getInstance(getXacmlResponse(response));
			AbstractResult result = responseCtx.getResults().iterator().next();
			if (AbstractResult.DECISION_PERMIT == result.getDecision()) {
				System.out.println("\n" + user_id
						+ " is authorized to perform this action\n\n");
			} else {
				System.out.println("\n" + user_id
						+ " is NOT authorized to perform this action\n");
				
				
				List<Advice> advices = result.getAdvices();
				for (Advice advice : advices) {
					List<AttributeAssignment> assignments = advice
							.getAssignments();
					for (AttributeAssignment assignment : assignments) {
						System.out.println("Advice :  "
								+ assignment.getContent() + "\n\n");
					}
				}
			}
		} catch (ParsingException e) {
			e.printStackTrace();
		}

		List<Obligation> sysObl = ResponseParser.getObligationsFromResponse(response);
		for(Obligation o : sysObl)
		{
			//Obligation.perform(o, xd);
		}
	}

	/**
	 * Creates DOM representation of the XACML request
	 * 
	 * @param response
	 *            XACML request as a String object
	 * @return XACML request as a DOM element
	 */
	public static Element getXacmlResponse(String response) {

		ByteArrayInputStream inputStream;
		DocumentBuilderFactory dbf;
		Document doc;

		inputStream = new ByteArrayInputStream(response.getBytes());
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);

		try {
			doc = dbf.newDocumentBuilder().parse(inputStream);
		} catch (Exception e) {
			System.err
					.println("DOM of request element can not be created from String");
			return null;
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				System.err
						.println("Error in closing input stream of XACML response");
			}
		}
		return doc.getDocumentElement();
	}
}
