/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.importer.gramps;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/** Reading helpers for the DOM of a Gramps XML file (the names are the local ones, whatever the namespace of the version is). */
final class GrampsXML{

	private GrampsXML(){}


	/**
	 * Parses the document without reading anything outside it: the DTD that the file declares (and that is on the web) is not
	 * fetched.
	 */
	static Document parse(final byte[] xml) throws IOException, SAXException, ParserConfigurationException{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

		final DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setEntityResolver((publicId, systemId) -> new InputSource(new ByteArrayInputStream(new byte[0])));
		return builder.parse(new ByteArrayInputStream(xml));
	}

	/** @return	The name of the element, without namespace. */
	static String name(final Element element){
		return (element.getLocalName() != null? element.getLocalName(): element.getNodeName());
	}

	/** @return	The value of the attribute, or {@code null} if missing or empty. */
	static String attribute(final Element element, final String name){
		return (element != null? StringUtils.trimToNull(element.getAttribute(name)): null);
	}

	/** @return	The child elements with the name, in document order. */
	static List<Element> children(final Element parent, final String name){
		final List<Element> children = new ArrayList<>();
		if(parent == null)
			return children;

		final NodeList nodes = parent.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++)
			if(nodes.item(i) instanceof Element child && name.equals(name(child)))
				children.add(child);
		return children;
	}

	/** @return	All the child elements, in document order. */
	static List<Element> children(final Element parent){
		final List<Element> children = new ArrayList<>();
		if(parent == null)
			return children;

		final NodeList nodes = parent.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++)
			if(nodes.item(i).getNodeType() == Node.ELEMENT_NODE)
				children.add((Element)nodes.item(i));
		return children;
	}

	/** @return	The first child element with the name, or {@code null}. */
	static Element child(final Element parent, final String name){
		final List<Element> children = children(parent, name);
		return (!children.isEmpty()? children.getFirst(): null);
	}

	/** @return	The text of the element, or {@code null} if missing or empty. */
	static String text(final Element element){
		return (element != null? StringUtils.trimToNull(element.getTextContent()): null);
	}

	/** @return	The text of the first child element with the name, or {@code null}. */
	static String childText(final Element parent, final String name){
		return text(child(parent, name));
	}

}
