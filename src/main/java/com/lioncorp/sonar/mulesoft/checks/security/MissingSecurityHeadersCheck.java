package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing security headers.
 */
@Rule(key = "MS026")
public class MissingSecurityHeadersCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS026";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows with HTTP listeners for missing security headers
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (hasHttpListener(flow)) {
        checkForMissingSecurityHeaders(context, inputFile, flow);
      }
    }
  }

  private boolean hasHttpListener(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    org.w3c.dom.NodeList listeners = flow.element.getElementsByTagNameNS("*", "listener");
    for (int i = 0; i < listeners.getLength(); i++) {
      org.w3c.dom.Element listener = (org.w3c.dom.Element) listeners.item(i);
      String namespaceURI = listener.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("http")) {
        return true;
      }
    }

    return false;
  }

  private void checkForMissingSecurityHeaders(SensorContext context, InputFile inputFile, MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return;
    }

    String flowContent = flow.element.getTextContent();
    if (flowContent == null) {
      return;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for important security headers
    boolean hasXFrameOptions = lowerContent.contains("x-frame-options");
    boolean hasXContentTypeOptions = lowerContent.contains("x-content-type-options");
    boolean hasStrictTransportSecurity = lowerContent.contains("strict-transport-security") ||
                                        lowerContent.contains("hsts");
    boolean hasContentSecurityPolicy = lowerContent.contains("content-security-policy");
    boolean hasXXSSProtection = lowerContent.contains("x-xss-protection");

    if (!hasXFrameOptions && !hasXContentTypeOptions && !hasStrictTransportSecurity &&
        !hasContentSecurityPolicy && !hasXXSSProtection) {
      reportIssue(context, inputFile,
          "Flow '" + flow.name + "' returns HTTP responses without security headers. " +
          "Add security headers: X-Frame-Options, X-Content-Type-Options, Strict-Transport-Security, Content-Security-Policy, X-XSS-Protection.");
    }
  }
}
