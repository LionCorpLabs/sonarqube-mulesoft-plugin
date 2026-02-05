package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

/**
 * Obsolete configuration.
 */
@Rule(key = "MS058")
public class ObsoleteConfigurationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS058";
  }
  private static final Map<String, String> DEPRECATED_ELEMENTS = new HashMap<>();
  private static final Map<String, String> DEPRECATED_ATTRIBUTES = new HashMap<>();

  static {
    // Deprecated elements (element name -> replacement)
    DEPRECATED_ELEMENTS.put("http:inbound-endpoint", "http:listener");
    DEPRECATED_ELEMENTS.put("http:outbound-endpoint", "http:request");
    DEPRECATED_ELEMENTS.put("message-properties-transformer", "set-variable or set-payload");
    DEPRECATED_ELEMENTS.put("expression-transformer", "transform message");
    DEPRECATED_ELEMENTS.put("custom-processor", "java:invoke");
    DEPRECATED_ELEMENTS.put("poll", "scheduler");
    DEPRECATED_ELEMENTS.put("tcp:connector", "sockets:listener-config");
    DEPRECATED_ELEMENTS.put("jdbc-ee:connector", "db:config");

    // Deprecated attributes (attribute name -> replacement)
    DEPRECATED_ATTRIBUTES.put("processingStrategy", "maxConcurrency");
    DEPRECATED_ATTRIBUTES.put("doc:name", "name (for flow elements)");
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument != null) {
      scanElement(parsedFile.xmlDocument.getDocumentElement(), context, inputFile);
    }
  }

  private void scanElement(Element element, SensorContext context, InputFile inputFile) {
    String tagName = element.getTagName();

    // Check for deprecated elements
    for (Map.Entry<String, String> entry : DEPRECATED_ELEMENTS.entrySet()) {
      String deprecated = entry.getKey();
      String replacement = entry.getValue();
      if (tagName.equals(deprecated) || tagName.endsWith(":" + deprecated)) {
        reportIssue(context, inputFile,
            "Deprecated element '" + deprecated + "' detected. " +
            "Replace with '" + replacement + "' for better compatibility with newer MuleSoft versions.");
      }
    }

    // Check for deprecated attributes
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attr = attributes.item(i);
      String attrName = attr.getNodeName();
      if (DEPRECATED_ATTRIBUTES.containsKey(attrName)) {
        reportIssue(context, inputFile,
            "Deprecated attribute '" + attrName + "' detected in element '" + tagName + "'. " +
            "Consider using '" + DEPRECATED_ATTRIBUTES.get(attrName) + "' instead.");
      }
    }

    // Check for old namespace versions (e.g., http://www.mulesoft.org/schema/mule/http from older versions)
    String namespaceURI = element.getNamespaceURI();
    if (namespaceURI != null) {
      if (namespaceURI.contains("/schema/mule/core/3") ||
          namespaceURI.contains("/schema/mule/http/3") ||
          namespaceURI.contains("/schema/mule/db/3")) {
        reportIssue(context, inputFile,
            "Obsolete Mule 3 namespace detected: '" + namespaceURI + "'. " +
            "Update to Mule 4 namespaces for compatibility.");
      }
    }

    // Recursively check child elements
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element) {
        scanElement((Element) children.item(i), context, inputFile);
      }
    }
  }
}
