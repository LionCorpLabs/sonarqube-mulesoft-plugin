package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Insecure CORS configuration.
 */
@Rule(key = "MS020")
public class InsecureCORSConfigurationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS020";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP configurations for insecure CORS settings
    for (MuleSoftFileParser.HttpConfiguration config : parsedFile.httpConfigurations) {
      if (config.element != null) {
        checkCORSConfiguration(context, inputFile, config.element);
      }
    }

    // Check raw content for CORS headers
    if (parsedFile.xmlDocument != null) {
      checkElementForCORSHeaders(context, inputFile, parsedFile.xmlDocument.getDocumentElement());
    }
  }

  private void checkCORSConfiguration(SensorContext context, InputFile inputFile, org.w3c.dom.Element element) {
    // Check for Access-Control-Allow-Origin header
    org.w3c.dom.NodeList headers = element.getElementsByTagName("header");
    for (int i = 0; i < headers.getLength(); i++) {
      org.w3c.dom.Element header = (org.w3c.dom.Element) headers.item(i);
      String headerName = header.getAttribute("headerName");
      String headerValue = header.getAttribute("value");

      if ("Access-Control-Allow-Origin".equalsIgnoreCase(headerName)) {
        if ("*".equals(headerValue)) {
          reportIssue(context, inputFile,
              "Insecure CORS configuration: Access-Control-Allow-Origin set to '*' (wildcard). " +
              "Specify explicit allowed origins to prevent unauthorized cross-origin requests.");
        }
      }

      if ("Access-Control-Allow-Credentials".equalsIgnoreCase(headerName)) {
        if ("true".equalsIgnoreCase(headerValue)) {
          // Check if origin is wildcard
          if (hasWildcardOrigin(element)) {
            reportIssue(context, inputFile,
                "Dangerous CORS configuration: Access-Control-Allow-Credentials enabled with wildcard origin. " +
                "This allows any origin to make credentialed requests.");
          }
        }
      }
    }
  }

  private boolean hasWildcardOrigin(org.w3c.dom.Element element) {
    org.w3c.dom.NodeList headers = element.getElementsByTagName("header");
    for (int i = 0; i < headers.getLength(); i++) {
      org.w3c.dom.Element header = (org.w3c.dom.Element) headers.item(i);
      String headerName = header.getAttribute("headerName");
      String headerValue = header.getAttribute("value");

      if ("Access-Control-Allow-Origin".equalsIgnoreCase(headerName) && "*".equals(headerValue)) {
        return true;
      }
    }
    return false;
  }

  private void checkElementForCORSHeaders(SensorContext context, InputFile inputFile, org.w3c.dom.Element element) {
    // Check for set-property or set-variable with CORS headers
    String tagName = element.getTagName();
    if ("set-property".equals(tagName) || "set-variable".equals(tagName)) {
      String propertyName = element.getAttribute("propertyName");
      String variableName = element.getAttribute("variableName");
      String value = element.getAttribute("value");

      String name = propertyName != null ? propertyName : variableName;

      if ("Access-Control-Allow-Origin".equalsIgnoreCase(name) && "*".equals(value)) {
        reportIssue(context, inputFile,
            "Insecure CORS: Access-Control-Allow-Origin set to wildcard '*'. Use specific origins for security.");
      }
    }

    // Check child elements recursively
    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        checkElementForCORSHeaders(context, inputFile, (org.w3c.dom.Element) child);
      }
    }
  }
}
