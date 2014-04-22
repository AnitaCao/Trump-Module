package org.openmrs.module.trumpmodule.dataFinder;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import luca.data.XmlDataHandler;

public class OpenmrsPolicyDataHandler extends XmlDataHandler {
	
	File newFile;
	DocumentBuilderFactory dbFactory;
	DocumentBuilder dBuilder;
	Document doc;

	public OpenmrsPolicyDataHandler(String filePath) throws ParserConfigurationException, SAXException, IOException  {
		super(filePath);
		newFile = new File(filePath);
		dbFactory = DocumentBuilderFactory.newInstance();
	
		dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(newFile);
		doc.getDocumentElement().normalize();
		
	}
	
	public void getTestString(){
		try{
			System.err.println("root of xml file" + doc.getDocumentElement().getNodeName());
			NodeList nodes = doc.getElementsByTagName("PolicySet");

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);

				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;
					System.out.println("Description: " + getValue("Description", element));
	
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	private static String getValue(String tag, Element element) {
		NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
		Node node = (Node) nodes.item(0);
		return node.getNodeValue();
	}
	
}


