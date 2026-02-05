package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Inefficient DataWeave.
 */
@Rule(key = "MS074")
public class IneffientDataWeaveTransformationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS074";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Find all DataWeave transformations
    org.w3c.dom.NodeList transformNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "transform");

    for (int i = 0; i < transformNodes.getLength(); i++) {
      org.w3c.dom.Element transformElement = (org.w3c.dom.Element) transformNodes.item(i);

      // Extract DataWeave script content
      String dwScript = extractDataWeaveScript(transformElement);

      if (dwScript != null) {
        checkForInefficientPatterns(context, inputFile, dwScript);
      }
    }

    // Also check set-payload elements with DataWeave expressions
    org.w3c.dom.NodeList setPayloadNodes = parsedFile.xmlDocument.getElementsByTagName("set-payload");
    for (int i = 0; i < setPayloadNodes.getLength(); i++) {
      org.w3c.dom.Element setPayloadElement = (org.w3c.dom.Element) setPayloadNodes.item(i);
      String value = setPayloadElement.getAttribute("value");

      if (value != null && (value.contains("#[") || value.contains("%dw"))) {
        checkForInefficientPatterns(context, inputFile, value);
      }
    }
  }

  private String extractDataWeaveScript(org.w3c.dom.Element transformElement) {
    // Look for the script content in child elements or CDATA
    org.w3c.dom.NodeList children = transformElement.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE || child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
        String content = child.getTextContent();
        if (content != null && content.trim().length() > 0) {
          return content;
        }
      } else if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;
        if ("message".equals(childElement.getLocalName()) || "set-payload".equals(childElement.getLocalName())) {
          return extractDataWeaveScript(childElement);
        }
      }
    }
    return null;
  }

  private void checkForInefficientPatterns(SensorContext context, InputFile inputFile, String dwScript) {
    // Check for nested map operations (map within map)
    if (dwScript.matches("(?s).*map\\s*\\(.*map\\s*\\(.*")) {
      reportIssue(context, inputFile,
          "Nested map operations detected in DataWeave script. " +
          "Consider using flatMap or refactoring to improve performance.");
    }

    // Check for multiple filter operations in sequence
    if (countOccurrences(dwScript, "filter") > 2) {
      reportIssue(context, inputFile,
          "Multiple filter operations detected in DataWeave script. " +
          "Consider combining filters into a single operation with compound conditions.");
    }

    // Check for unnecessary payload materialization (payload.^raw)
    if (dwScript.contains("payload.^raw")) {
      reportIssue(context, inputFile,
          "Unnecessary payload materialization detected (payload.^raw). " +
          "This can cause performance issues with large payloads.");
    }

    // Check for sizeOf in loops or repetitive operations
    if (dwScript.matches("(?s).*map\\s*\\(.*sizeOf.*") || dwScript.matches("(?s).*filter\\s*\\(.*sizeOf.*")) {
      reportIssue(context, inputFile,
          "sizeOf function used within iterative operations. " +
          "Consider calculating size once and storing in a variable.");
    }
  }

  private int countOccurrences(String text, String pattern) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(pattern, index)) != -1) {
      count++;
      index += pattern.length();
    }
    return count;
  }
}
