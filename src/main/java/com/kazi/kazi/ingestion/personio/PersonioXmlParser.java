package com.kazi.kazi.ingestion.personio;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

final class PersonioXmlParser {

    private PersonioXmlParser() {
    }

    static List<PersonioPosition> parse(String xml)
        throws ParserConfigurationException, SAXException, IOException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        var positions = document.getElementsByTagName("position");
        List<PersonioPosition> result = new ArrayList<>(positions.getLength());
        for (int index = 0; index < positions.getLength(); index++) {
            result.add(position((Element) positions.item(index)));
        }
        return result;
    }

    private static PersonioPosition position(Element element) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        var descriptionsElement = child(element, "jobDescriptions");
        if (descriptionsElement != null) {
            var values = descriptionsElement.getElementsByTagName("jobDescription");
            for (int index = 0; index < values.getLength(); index++) {
                var description = (Element) values.item(index);
                descriptions.put(childText(description, "name"), childText(description, "value"));
            }
        }

        List<String> additionalOffices = new ArrayList<>();
        var additionalOfficesElement = child(element, "additionalOffices");
        if (additionalOfficesElement != null) {
            var offices = additionalOfficesElement.getElementsByTagName("office");
            for (int index = 0; index < offices.getLength(); index++) {
                additionalOffices.add(offices.item(index).getTextContent().trim());
            }
        }

        return new PersonioPosition(
            childText(element, "id"),
            childText(element, "name"),
            childText(element, "office"),
            List.copyOf(additionalOffices),
            childText(element, "employmentType"),
            childText(element, "schedule"),
            childText(element, "createdAt"),
            Map.copyOf(descriptions)
        );
    }

    private static String childText(Element parent, String tagName) {
        var child = child(parent, tagName);
        return child == null ? "" : child.getTextContent().trim();
    }

    private static Element child(Element parent, String tagName) {
        var children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }
}
