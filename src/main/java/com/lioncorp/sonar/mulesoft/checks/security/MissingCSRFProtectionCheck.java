package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing CSRF protection.
 */
@Rule(key = "MS013")
public class MissingCSRFProtectionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS013";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows with state-changing HTTP methods (POST, PUT, DELETE) for CSRF protection
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.element != null) {
        if (hasStateChangingHttpListener(flow) && !hasCSRFProtection(flow)) {
          reportIssue(context, inputFile,
              "Flow '" + flow.name + "' handles state-changing HTTP requests (POST/PUT/DELETE) without CSRF protection. " +
              "Implement token validation or use anti-CSRF headers to prevent Cross-Site Request Forgery attacks.");
        }
      }
    }
  }

  private boolean hasStateChangingHttpListener(MuleSoftFileParser.MuleSoftFlow flow) {
    org.w3c.dom.Element flowElement = flow.element;
    org.w3c.dom.NodeList listeners = flowElement.getElementsByTagNameNS("*", "listener");

    for (int i = 0; i < listeners.getLength(); i++) {
      org.w3c.dom.Element listener = (org.w3c.dom.Element) listeners.item(i);
      String allowedMethods = listener.getAttribute("allowedMethods");

      if (allowedMethods == null || allowedMethods.isEmpty()) {
        // If no methods specified, all methods are allowed
        return true;
      }

      String methodsUpper = allowedMethods.toUpperCase();
      if (methodsUpper.contains("POST") || methodsUpper.contains("PUT") || methodsUpper.contains("DELETE")) {
        return true;
      }
    }

    return false;
  }

  private boolean hasCSRFProtection(MuleSoftFileParser.MuleSoftFlow flow) {
    org.w3c.dom.Element flowElement = flow.element;
    String flowContent = flowElement.getTextContent();

    if (flowContent == null) {
      return false;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for common CSRF protection patterns
    return lowerContent.contains("csrf") ||
           lowerContent.contains("x-csrf-token") ||
           lowerContent.contains("csrftoken") ||
           lowerContent.contains("_csrf") ||
           (lowerContent.contains("token") && lowerContent.contains("validat"));
  }
}
