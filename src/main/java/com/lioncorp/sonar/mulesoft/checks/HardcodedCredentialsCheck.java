package com.lioncorp.sonar.mulesoft.checks;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Check for hardcoded credentials in MuleSoft XML files.
 */
@Rule(key = "HardcodedCredentials")
public class HardcodedCredentialsCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "HardcodedCredentials";
  }

  private static final List<String> CREDENTIAL_ATTRIBUTES = Arrays.asList(
      "password",
      "passwd",
      "pwd",
      "secret",
      "apiKey",
      "api-key",
      "token",
      "accessToken",
      "access-token",
      "clientSecret",
      "client-secret"
  );

  private static final Pattern HARDCODED_VALUE_PATTERN = Pattern.compile(
      "^(?!\\$\\{|#\\[).*", // Not a placeholder ${...} or expression #[...]
      Pattern.CASE_INSENSITIVE
  );

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument != null) {
      checkElement(context, inputFile, parsedFile.xmlDocument.getDocumentElement());
    }
  }

  private void checkElement(SensorContext context, InputFile inputFile, Element element) {
    // Check attributes
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attribute = attributes.item(i);
      String attrName = attribute.getNodeName();
      String attrValue = attribute.getNodeValue();

      if (isCredentialAttribute(attrName) && isHardcodedValue(attrValue)) {
        reportIssue(context, inputFile, attrName);
      }
    }

    // Check child elements recursively
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        checkElement(context, inputFile, (Element) child);
      }
    }
  }

  private boolean isCredentialAttribute(String attributeName) {
    String lowerName = attributeName.toLowerCase();
    return CREDENTIAL_ATTRIBUTES.stream()
        .anyMatch(lowerName::contains);
  }

  private boolean isHardcodedValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      return false;
    }

    // Allow placeholders like ${property} or expressions like #[expression]
    return !value.contains("${") && !value.contains("#[") && value.length() > 3;
  }
}
