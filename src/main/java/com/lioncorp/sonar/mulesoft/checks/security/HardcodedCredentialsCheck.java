package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.DomUtils;
import com.lioncorp.sonar.mulesoft.utils.PatternMatcher;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Check for hardcoded credentials in MuleSoft XML files.
 */
@Rule(key = "MS001")
public class HardcodedCredentialsCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS001";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument != null) {
      checkElement(context, inputFile, parsedFile.xmlDocument.getDocumentElement(), parsedFile.rawContent);
    }
  }

  private void checkElement(SensorContext context, InputFile inputFile, Element element, String xmlContent) {
    // Check attributes
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attribute = attributes.item(i);
      String attrName = attribute.getNodeName();
      String attrValue = attribute.getNodeValue();

      if (PatternMatcher.isCredentialAttribute(attrName) && isHardcodedValue(attrValue)) {
        int lineNumber = DomUtils.findLineNumber(element, xmlContent);
        reportIssue(context, inputFile, attrName, lineNumber);
      }
    }

    // Check child elements recursively
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        checkElement(context, inputFile, (Element) child, xmlContent);
      }
    }
  }

  private boolean isHardcodedValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      return false;
    }
    // Allow placeholders and expressions, but flag hardcoded values
    return PatternMatcher.isHardcodedValue(value) && value.length() > 3;
  }
}
