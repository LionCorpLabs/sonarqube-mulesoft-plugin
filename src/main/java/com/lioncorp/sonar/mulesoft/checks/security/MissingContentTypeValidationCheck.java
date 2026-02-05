package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing Content-Type validation.
 */
@Rule(key = "MS024")
public class MissingContentTypeValidationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS024";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows with HTTP listeners for missing Content-Type validation
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (hasHttpListenerWithBody(flow) && !hasContentTypeValidation(flow)) {
        reportIssue(context, inputFile,
            "Flow '" + flow.name + "' accepts HTTP requests with body but lacks Content-Type validation. " +
            "Validate Content-Type header to prevent content type confusion attacks and ensure proper data handling.");
      }
    }
  }

  private boolean hasHttpListenerWithBody(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    org.w3c.dom.NodeList listeners = flow.element.getElementsByTagNameNS("*", "listener");
    for (int i = 0; i < listeners.getLength(); i++) {
      org.w3c.dom.Element listener = (org.w3c.dom.Element) listeners.item(i);
      String allowedMethods = listener.getAttribute("allowedMethods");

      // If no methods specified or POST/PUT/PATCH are allowed (methods that typically have body)
      if (allowedMethods == null || allowedMethods.isEmpty()) {
        return true; // All methods allowed, including POST
      }

      String methodsUpper = allowedMethods.toUpperCase();
      if (methodsUpper.contains("POST") || methodsUpper.contains("PUT") || methodsUpper.contains("PATCH")) {
        return true;
      }
    }

    return false;
  }

  private boolean hasContentTypeValidation(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    String flowContent = flow.element.getTextContent();
    if (flowContent == null) {
      return false;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for Content-Type validation patterns
    return lowerContent.contains("content-type") ||
           lowerContent.contains("contenttype") ||
           lowerContent.contains("mediatype") ||
           lowerContent.contains("media-type") ||
           (lowerContent.contains("header") && lowerContent.contains("application/json"));
  }
}
