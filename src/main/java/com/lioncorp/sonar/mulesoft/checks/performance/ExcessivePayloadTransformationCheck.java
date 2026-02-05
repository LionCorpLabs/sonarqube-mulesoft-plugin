package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Excessive transformations.
 */
@Rule(key = "MS080")
public class ExcessivePayloadTransformationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS080";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check each flow for excessive consecutive transformations
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      java.util.List<org.w3c.dom.Element> transformations = findTransformations(flow.element);

      if (transformations.size() > MAX_CONSECUTIVE_TRANSFORMATIONS) {
        reportIssue(context, inputFile,
            String.format("Flow '%s' contains %d consecutive payload transformations. " +
                "Multiple transformations on the same payload can be inefficient. " +
                "Consider combining transformations into a single operation.",
                flow.name, transformations.size()));
      }
    }
  }

  private static final int MAX_CONSECUTIVE_TRANSFORMATIONS = 3;

  private java.util.List<org.w3c.dom.Element> findTransformations(org.w3c.dom.Element element) {
    java.util.List<org.w3c.dom.Element> transformations = new java.util.ArrayList<>();

    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;

        if (isTransformationElement(childElement)) {
          transformations.add(childElement);
        } else {
          // Recursively check nested elements
          transformations.addAll(findTransformations(childElement));
        }
      }
    }

    return transformations;
  }

  private boolean isTransformationElement(org.w3c.dom.Element element) {
    String localName = element.getLocalName();
    String namespaceURI = element.getNamespaceURI();

    // DataWeave transformations
    if ("transform".equals(localName) || "set-payload".equals(localName) ||
        "set-variable".equals(localName) || "ee:transform".equals(element.getTagName())) {
      return true;
    }

    // Check for DataWeave namespace
    if (namespaceURI != null && namespaceURI.contains("ee/core")) {
      return true;
    }

    // Other transformation elements
    return "json-to-object-transformer".equals(localName) ||
           "xml-to-object-transformer".equals(localName) ||
           "object-to-json-transformer".equals(localName) ||
           "object-to-xml-transformer".equals(localName) ||
           "expression-transformer".equals(localName);
  }
}
