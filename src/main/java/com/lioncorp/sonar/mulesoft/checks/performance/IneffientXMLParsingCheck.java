package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Inefficient XML parsing.
 */
@Rule(key = "MS083")
public class IneffientXMLParsingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS083";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check for DOM-based XML parsing without streaming
    org.w3c.dom.NodeList xmlToObjectNodes = parsedFile.xmlDocument.getElementsByTagName("xml-to-object-transformer");
    if (xmlToObjectNodes.getLength() > 0) {
      reportIssue(context, inputFile,
          "xml-to-object-transformer detected. For large XML files, consider using streaming XML parsers " +
          "(StAX) instead of DOM-based parsing to reduce memory consumption.");
    }

    // Check for mulexml transformations without streaming
    org.w3c.dom.NodeList muleXmlNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "xml-to-object");
    for (int i = 0; i < muleXmlNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) muleXmlNodes.item(i);
      if (!hasStreamingEnabled(element)) {
        reportIssue(context, inputFile,
            "XML parsing without streaming detected. Consider enabling streaming for large XML files " +
            "to improve performance and reduce memory usage.");
      }
    }

    // Check for XPath queries in loops
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      checkForXPathInLoop(context, inputFile, flow.element);
    }

    // Check for JAXB unmarshalling without streaming
    org.w3c.dom.NodeList jaxbNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "jaxb");
    for (int i = 0; i < jaxbNodes.getLength(); i++) {
      org.w3c.dom.Element jaxbElement = (org.w3c.dom.Element) jaxbNodes.item(i);
      String operation = jaxbElement.getAttribute("operation");
      if ("unmarshal".equals(operation) && !hasStreamingEnabled(jaxbElement)) {
        reportIssue(context, inputFile,
            "JAXB unmarshalling detected without streaming. For large XML documents, " +
            "consider using streaming or incremental parsing.");
      }
    }
  }

  private boolean hasStreamingEnabled(org.w3c.dom.Element element) {
    String streaming = element.getAttribute("streaming");
    return "true".equalsIgnoreCase(streaming);
  }

  private void checkForXPathInLoop(SensorContext context, InputFile inputFile, org.w3c.dom.Element element) {
    // Check if this is a foreach element
    org.w3c.dom.NodeList foreachNodes = element.getElementsByTagName("foreach");

    for (int i = 0; i < foreachNodes.getLength(); i++) {
      org.w3c.dom.Element foreachElement = (org.w3c.dom.Element) foreachNodes.item(i);

      // Check if there are XPath operations inside foreach
      if (containsXPathOperation(foreachElement)) {
        reportIssue(context, inputFile,
            "XPath query detected inside foreach loop. This can cause performance issues. " +
            "Consider extracting data once before the loop or using more efficient query methods.");
      }
    }
  }

  private boolean containsXPathOperation(org.w3c.dom.Element element) {
    // Check for XPath in DataWeave expressions
    String textContent = element.getTextContent();
    if (textContent != null && (textContent.contains("xpath(") || textContent.contains("XPath"))) {
      return true;
    }

    // Check for xpath attribute
    if (element.hasAttribute("xpath")) {
      return true;
    }

    // Check child elements recursively
    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        if (containsXPathOperation((org.w3c.dom.Element) child)) {
          return true;
        }
      }
    }

    return false;
  }
}
