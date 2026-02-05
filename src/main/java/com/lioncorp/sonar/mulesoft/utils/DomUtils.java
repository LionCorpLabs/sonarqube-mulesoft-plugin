package com.lioncorp.sonar.mulesoft.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for DOM traversal and XML element operations.
 */
public class DomUtils {

  private DomUtils() {
    // Utility class
  }

  /**
   * Get all child elements (excluding text nodes, comments, etc.).
   *
   * @param element the parent element
   * @return list of child elements
   */
  public static List<Element> getChildElements(Element element) {
    List<Element> children = new ArrayList<>();
    NodeList nodeList = element.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        children.add((Element) node);
      }
    }
    return children;
  }

  /**
   * Count the number of child elements (excluding text nodes).
   *
   * @param element the parent element
   * @return number of child elements
   */
  public static int getChildElementCount(Element element) {
    return getChildElements(element).size();
  }

  /**
   * Get all elements with a specific tag name recursively.
   *
   * @param doc the XML document
   * @param tagName the tag name to search for
   * @return list of matching elements
   */
  public static List<Element> getElementsByTagName(Document doc, String tagName) {
    List<Element> elements = new ArrayList<>();
    NodeList nodeList = doc.getElementsByTagName(tagName);
    for (int i = 0; i < nodeList.getLength(); i++) {
      elements.add((Element) nodeList.item(i));
    }
    return elements;
  }

  /**
   * Get all elements with a specific tag name using namespace.
   *
   * @param doc the XML document
   * @param namespaceURI the namespace URI (use "*" for any namespace)
   * @param localName the local name
   * @return list of matching elements
   */
  public static List<Element> getElementsByTagNameNS(Document doc, String namespaceURI, String localName) {
    List<Element> elements = new ArrayList<>();
    NodeList nodeList = doc.getElementsByTagNameNS(namespaceURI, localName);
    for (int i = 0; i < nodeList.getLength(); i++) {
      elements.add((Element) nodeList.item(i));
    }
    return elements;
  }

  /**
   * Check if an element has a specific attribute with a non-empty value.
   *
   * @param element the element to check
   * @param attributeName the attribute name
   * @return true if attribute exists and is not empty
   */
  public static boolean hasAttribute(Element element, String attributeName) {
    String value = element.getAttribute(attributeName);
    return value != null && !value.trim().isEmpty();
  }

  /**
   * Get attribute value with default fallback.
   *
   * @param element the element
   * @param attributeName the attribute name
   * @param defaultValue the default value if attribute doesn't exist
   * @return attribute value or default
   */
  public static String getAttributeOrDefault(Element element, String attributeName, String defaultValue) {
    String value = element.getAttribute(attributeName);
    return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
  }

  /**
   * Check if an element is in a specific namespace.
   *
   * @param element the element to check
   * @param namespacePattern the namespace pattern (e.g., "http")
   * @return true if element namespace contains the pattern
   */
  public static boolean isInNamespace(Element element, String namespacePattern) {
    String namespaceURI = element.getNamespaceURI();
    return namespaceURI != null && namespaceURI.contains(namespacePattern);
  }

  /**
   * Recursively traverse all elements in the document.
   *
   * @param element the starting element
   * @param visitor the visitor function to apply to each element
   */
  public static void traverseElements(Element element, ElementVisitor visitor) {
    visitor.visit(element);
    List<Element> children = getChildElements(element);
    for (Element child : children) {
      traverseElements(child, visitor);
    }
  }

  /**
   * Get the depth of element nesting from a root element.
   *
   * @param element the element to check
   * @param root the root element
   * @return depth level (root = 0)
   */
  public static int getDepth(Element element, Element root) {
    if (element.equals(root)) {
      return 0;
    }
    Node parent = element.getParentNode();
    if (parent == null || parent.getNodeType() != Node.ELEMENT_NODE) {
      return -1;
    }
    int parentDepth = getDepth((Element) parent, root);
    return parentDepth >= 0 ? parentDepth + 1 : -1;
  }

  /**
   * Check if an element contains any child elements (not just text).
   *
   * @param element the element to check
   * @return true if has child elements
   */
  public static boolean hasChildElements(Element element) {
    return getChildElementCount(element) > 0;
  }

  /**
   * Get the text content of an element, trimmed.
   *
   * @param element the element
   * @return trimmed text content
   */
  public static String getTextContent(Element element) {
    String content = element.getTextContent();
    return content != null ? content.trim() : "";
  }

  /**
   * Find all elements matching a specific attribute value.
   *
   * @param root the root element to start searching from
   * @param attributeName the attribute name
   * @param attributeValue the attribute value to match
   * @return list of matching elements
   */
  public static List<Element> findElementsByAttribute(Element root, String attributeName, String attributeValue) {
    List<Element> matches = new ArrayList<>();
    traverseElements(root, element -> {
      if (attributeValue.equals(element.getAttribute(attributeName))) {
        matches.add(element);
      }
    });
    return matches;
  }

  /**
   * Check if element's attribute matches any pattern.
   *
   * @param element the element
   * @param attributeName the attribute name
   * @param patterns patterns to match against
   * @return true if attribute matches any pattern
   */
  public static boolean attributeMatchesAny(Element element, String attributeName, String... patterns) {
    String value = element.getAttribute(attributeName);
    if (value == null || value.trim().isEmpty()) {
      return false;
    }
    for (String pattern : patterns) {
      if (value.toLowerCase().contains(pattern.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Find the line number of an element in the XML content.
   * Since standard DOM parser doesn't preserve line numbers, we search for the element in the content.
   *
   * @param element the element to find
   * @param xmlContent the full XML content as string
   * @return the line number (1-based), or 1 if not found
   */
  public static int findLineNumber(Element element, String xmlContent) {
    try {
      // Build a unique identifier for this element
      String tagName = element.getTagName();
      String searchPattern = "<" + tagName;

      // Add key attributes to make it more unique
      StringBuilder patternBuilder = new StringBuilder(searchPattern);

      // Check for common identifying attributes
      String[] identifyingAttrs = {"name", "path", "config-ref", "class", "message", "protocol", "host", "port", "type", "id"};
      for (String attr : identifyingAttrs) {
        String attrValue = element.getAttribute(attr);
        if (attrValue != null && !attrValue.isEmpty()) {
          patternBuilder.append(" ").append(attr).append("=\"").append(attrValue).append("\"");
          break; // Use first identifying attribute found
        }
      }

      String uniquePattern = patternBuilder.toString();

      // Find the pattern in the XML content
      int index = xmlContent.indexOf(uniquePattern);
      if (index == -1) {
        // Fallback: just search for the tag name
        index = xmlContent.indexOf(searchPattern);
      }

      if (index >= 0) {
        // Count newlines before this index to get line number
        int lineNumber = 1;
        for (int i = 0; i < index; i++) {
          if (xmlContent.charAt(i) == '\n') {
            lineNumber++;
          }
        }
        return lineNumber;
      }
    } catch (Exception e) {
      // Silently return default if any error occurs
    }

    return 1; // Default to line 1 if not found
  }

  /**
   * Functional interface for element traversal.
   */
  @FunctionalInterface
  public interface ElementVisitor {
    void visit(Element element);
  }
}
