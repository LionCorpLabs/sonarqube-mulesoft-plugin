package com.lioncorp.sonar.mulesoft.utils;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Utility class for XML element manipulation and traversal.
 * Eliminates duplicated XML processing code across check classes.
 */
public class XmlUtils {

    private XmlUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Get all child elements of a parent element.
     * Filters out non-element nodes (text, comments, etc.)
     *
     * @param parent the parent element
     * @return list of child elements
     */
    public static List<Element> getChildElements(Element parent) {
        List<Element> children = new ArrayList<>();
        if (parent == null) {
            return children;
        }

        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node child = nodeList.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) child);
            }
        }
        return children;
    }

    /**
     * Visit all elements recursively using a visitor pattern.
     *
     * @param root    the root element to start from
     * @param visitor the visitor function to apply to each element
     */
    public static void visitElements(Element root, ElementVisitor visitor) {
        if (root == null || visitor == null) {
            return;
        }

        // Visit the current element
        visitor.visit(root);

        // Recursively visit children
        for (Element child : getChildElements(root)) {
            visitElements(child, visitor);
        }
    }

    /**
     * Check all attributes of an element against a predicate.
     *
     * @param element the element to check
     * @param checker predicate that tests attribute name and value
     * @param action  action to perform when predicate returns true
     */
    public static void checkAttributes(Element element, BiPredicate<String, String> checker, Consumer<String> action) {
        if (element == null || checker == null || action == null) {
            return;
        }

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();

            if (attrValue != null && checker.test(attrName, attrValue)) {
                action.accept(attrValue);
            }
        }
    }

    /**
     * Find all elements with specific tag names recursively.
     *
     * @param root     the root element to search from
     * @param tagNames the tag names to search for
     * @return list of matching elements
     */
    public static List<Element> findElementsByTag(Element root, String... tagNames) {
        List<Element> result = new ArrayList<>();
        if (root == null || tagNames == null || tagNames.length == 0) {
            return result;
        }

        visitElements(root, element -> {
            String tagName = element.getTagName();
            for (String searchTag : tagNames) {
                if (searchTag != null && searchTag.equals(tagName)) {
                    result.add(element);
                    break;
                }
            }
        });

        return result;
    }

    /**
     * Get the value of an attribute, or empty string if not present.
     *
     * @param element       the element
     * @param attributeName the attribute name
     * @return the attribute value or empty string
     */
    public static String getAttributeValue(Element element, String attributeName) {
        if (element == null || attributeName == null) {
            return "";
        }
        String value = element.getAttribute(attributeName);
        return value != null ? value : "";
    }

    /**
     * Check if an element has an attribute with a specific value.
     *
     * @param element       the element
     * @param attributeName the attribute name
     * @param value         the value to check for
     * @return true if the attribute has the specified value
     */
    public static boolean hasAttributeValue(Element element, String attributeName, String value) {
        if (element == null || attributeName == null || value == null) {
            return false;
        }
        String attrValue = element.getAttribute(attributeName);
        return value.equals(attrValue);
    }

    /**
     * Functional interface for visiting elements.
     */
    @FunctionalInterface
    public interface ElementVisitor {
        void visit(Element element);
    }
}
