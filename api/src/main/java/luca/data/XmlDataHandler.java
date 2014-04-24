package luca.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.balana.ParsingException;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.DateAttribute;
import org.wso2.balana.attr.DateTimeAttribute;
import org.wso2.balana.attr.DayTimeDurationAttribute;
import org.wso2.balana.attr.DoubleAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.xml.sax.SAXException;

/**
 * Implementation of DataHandler that retrieve data from an XML specified data
 * source
 * */
public class XmlDataHandler implements DataHandler {

	public static final String INDENT_VALUE = "4";
	private String resourcePath;

	private static Log log = LogFactory
			.getLog(FileBasedPolicyFinderModule.class);

	/**
	 * Constructor for XmlDataHandler
	 * 
	 * @param filePath
	 *            the path of the XML file data source. Support both relative
	 *            and absolute path
	 * */
	public XmlDataHandler(String filePath) {
		resourcePath = filePath;
	}

	@Override
	public synchronized BagAttribute getBagAttribute(String entity,
			List<AttributeQuery> attributeQuery, String attributeName,
			String attributeType) {

		List<String> stringAttributes = getAttribute(entity, attributeQuery,
				attributeName);
		List<AttributeValue> values = new ArrayList<AttributeValue>();

		for (String str : stringAttributes) {
			if (attributeType.equals(StringAttribute.identifier))
				values.add(StringAttribute.getInstance(str));
			else if (attributeType.equals(DateAttribute.identifier)) {
				try {
					values.add(DateAttribute.getInstance(str));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (attributeType.equals(DateTimeAttribute.identifier)) {
				try {
					values.add(DateTimeAttribute.getInstance(str));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParsingException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (attributeType
					.equals(DayTimeDurationAttribute.identifier)) {
				try {
					values.add(DayTimeDurationAttribute.getInstance(str));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParsingException e) {
					e.printStackTrace();
				}
			} else if (attributeType.equals(DoubleAttribute.identifier)) {
				values.add(DoubleAttribute.getInstance(str));
			}

			// TODO add support for different DataTypes
		}

		BagAttribute bag = null;
		try {
			bag = new BagAttribute(new URI(attributeType), values);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return bag;
	}

	@Override
	public synchronized List<String> getAttribute(String entity,
			List<AttributeQuery> attributeQuery, String attributeName) {

		String attQuery = "";
		// attributeQuery.clear();

		for (int i = 0; i < attributeQuery.size(); i++) {
			AttributeQuery app = attributeQuery.get(i);
			if (i == 0)
				attQuery = "[";
			else
				attQuery += " and ";
			attQuery += app.toString();
		}

		if (attributeQuery.size() > 0)
			attQuery += "]";

		String xPath = "//" + entity + attQuery + "/" + attributeName
				+ "/text()";

		List<String> values = getDataFromXPath(resourcePath, xPath);

		return values;
	}

	@Override
	public synchronized boolean contains(String entity, String id) {
		ArrayList<AttributeQuery> aq = new ArrayList<AttributeQuery>();
		List<String> ids = getAttribute(entity, aq, "id");
		return ids.contains(id);
	}

	private synchronized String attributeListId(List<AttributeQuery> attributes) {
		String id = null;

		for (AttributeQuery aq : attributes) {
			if (aq.name.equals("id"))
				id = aq.value;
		}
		return id;
	}

	@Override
	public String getNextId(String entity) {
		List<String> ids = XmlDataHandler.getDataFromXPath(resourcePath, "//"
				+ entity + "/id/text()");
		ArrayList<Integer> idsInt = new ArrayList<Integer>();
		for (String str : ids) {
			idsInt.add(Integer.parseInt(str));
		}
		Integer max;
		if (idsInt.size() > 0)
			max = Collections.max(idsInt);
		else
			max = 0;
		return "" + (max.intValue() + 1);
	}

	@Override
	public synchronized String write(String entity,
			List<AttributeQuery> attributes) {

		String id = attributeListId(attributes);

		if (id == null) {
			id = getNextId(entity);
		}

		if (contains(entity, id)) {
			log.debug("XmlDataHandler.write:\nobject of the entity:" + entity
					+ " with id:" + id + " already present");
			return null;
		}

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();

		DocumentBuilder documentBuilder;
		Document document;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			document = documentBuilder.parse(resourcePath);
		} catch (SAXException e1) {
			e1.printStackTrace();
			return null;
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		} catch (ParserConfigurationException e2) {
			e2.printStackTrace();
			return null;
		}

		// Root Element
		Element rootElement = document.getDocumentElement();

		// new element
		Element entityNode = document.createElement(entity);
		Element idEl = document.createElement("id");
		idEl.appendChild(document.createTextNode(id));
		entityNode.appendChild(idEl);

		for (AttributeQuery aq : attributes) {
			if (!aq.name.equals("id")) {
				Element el = document.createElement(aq.name);
				el.appendChild(document.createTextNode(aq.value));
				entityNode.appendChild(el);
			}
		}

		rootElement.appendChild(entityNode);

		DOMSource source = new DOMSource(document);

		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", INDENT_VALUE);
			StreamResult result = new StreamResult(resourcePath);
			transformer.transform(source, result);

		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return id;
	}

	@Override
	public synchronized boolean addAttribute(String entity, String id,
			List<AttributeQuery> attributes) {
		String xPath = "//" + entity + "[id=\"" + id + "\"]";
		NodeList nodes = null;
		Document doc = null;

		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse(resourcePath);

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(xPath);

			Object evalResult = expr.evaluate(doc, XPathConstants.NODESET);

			// convert nodes in AttributeValues
			nodes = (NodeList) evalResult;
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("error retrieving resource from xml");
		}

		if (nodes.getLength() == 0) {
			log.debug("XmlDataHandler.addAttribute: entity:" + entity
					+ " with id:" + id + " not present");
			return false;
		}

		Node node = nodes.item(0);

		for (AttributeQuery aq : attributes) {
			Element el = doc.createElement(aq.name);
			el.appendChild(doc.createTextNode(aq.value));
			node.appendChild(el);
		}

		DOMSource source = new DOMSource(doc);

		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
			StreamResult result = new StreamResult(resourcePath);
			transformer.transform(source, result);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", INDENT_VALUE);

		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public synchronized boolean modifyAttribute(String entity, String id,
			List<AttributeQuery> attributes) {
		String xPath = "//" + entity + "[id=\"" + id + "\"]";
		NodeList nodes = null;
		Document doc = null;

		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse(resourcePath);

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(xPath);

			Object evalResult = expr.evaluate(doc, XPathConstants.NODESET);

			// convert nodes in AttributeValues
			nodes = (NodeList) evalResult;
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("error retrieving resource from xml");
		}

		if (nodes.getLength() == 0) {
			log.debug("XmlDataHandler.addAttribute: entity:" + entity
					+ " with id:" + id + " not present");
			return false;
		}

		Node entityNode = nodes.item(0);

		for (AttributeQuery aq : attributes) {
			NodeList attributeNodes = entityNode.getChildNodes();
			boolean done = false;
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				Node old = attributeNodes.item(i);
				if (aq.name.equals(old.getNodeName())) {
					Element ne = doc.createElement(aq.name);
					ne.setTextContent(aq.value);
					entityNode.removeChild(old);
					entityNode.appendChild(ne);
					done = true;
				}
			}
			if (!done)
				return false;
		}

		DOMSource source = new DOMSource(doc);

		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
			StreamResult result = new StreamResult(resourcePath);
			transformer.transform(source, result);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", INDENT_VALUE);

		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public synchronized boolean remove(String entity, String id) {
		String xPath = "//" + entity + "[id=\"" + id + "\"]";
		NodeList nodes = null;
		Document doc = null;

		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse(resourcePath);

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(xPath);

			Object evalResult = expr.evaluate(doc, XPathConstants.NODESET);

			// convert nodes in AttributeValues
			nodes = (NodeList) evalResult;
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("error retrieving resource from xml");
		}

		if (nodes.getLength() == 0) {
			log.debug("XmlDataHandler.remove: entity:" + entity + " with id:"
					+ id + " not present");
			return false;
		}

		Node node = nodes.item(0);
		node.getParentNode().removeChild(node);

		DOMSource source = new DOMSource(doc);

		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
			StreamResult result = new StreamResult(resourcePath);
			transformer.transform(source, result);
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "2");

		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * static method that retrieve data from an XML data source
	 * 
	 * @param filePath
	 *            the path of the data source
	 * @xPath xPath expression for the attribute in the data source
	 * **/
	public synchronized static ArrayList<String> getDataFromXPath(
			String filePath, String xPath) {
		ArrayList<String> results = new ArrayList<String>();

		NodeList nodes = getNodeListFromXpath(filePath, xPath);
		for (int i = 0; i < nodes.getLength(); i++) {
			results.add(nodes.item(i).getTextContent());
		}
		return results;
	}

	/**
	 * static method that retrieve NodeList from an XML data source
	 * 
	 * @param filePath
	 *            the path of the data source
	 * @param xPath
	 *            expression for the attribute in the data source
	 * @return NodeList result of the xPath expression
	 * **/
	public synchronized static NodeList getNodeListFromXpathString(
			String xmlCode, String xPath) {

		NodeList nodes = null;
		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(xmlCode
					.getBytes()));

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(xPath);

			Object evalResult = expr.evaluate(doc, XPathConstants.NODESET);

			// convert nodes in AttributeValues
			nodes = (NodeList) evalResult;
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("error retrieving resource from xml");
		}
		return nodes;
	}

	public synchronized static NodeList getNodeListFromXpath(String filePath,
			String xPath) {

		NodeList nodes = null;
		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(filePath);

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(xPath);
			Object evalResult = expr.evaluate(doc, XPathConstants.NODESET);

			// convert nodes in AttributeValues
			nodes = (NodeList) evalResult;
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("error retrieving resource from xml");
		}
		return nodes;
	}

	@Override
	public synchronized List<AttributeQuery> getAttributesOf(String entity,
			String id) {
		ArrayList<AttributeQuery> attributes = new ArrayList<AttributeQuery>();
		String xPath = "//" + entity + "[id=" + "\"" + id + "\"]";
		NodeList nodelist = getNodeListFromXpath(resourcePath, xPath);
		if (nodelist.getLength() == 0)
			return null;
		Node node = nodelist.item(0);
		NodeList childs = node.getChildNodes();

		for (int i = 0; i < childs.getLength(); i++) {
			Node childNode = childs.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				attributes
						.add(new AttributeQuery(childNode.getNodeName(),
								childNode.getTextContent(),
								StringAttribute.identifier));
			}
		}
		return attributes;
	}
}
