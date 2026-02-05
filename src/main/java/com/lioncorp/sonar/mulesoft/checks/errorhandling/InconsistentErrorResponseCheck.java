package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Inconsistent error response.
 */
@Rule(key = "MS092")
public class InconsistentErrorResponseCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS092";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Track error response formats across all error handlers
    java.util.Set<String> responseFormats = new java.util.HashSet<>();
    java.util.Set<String> statusCodePatterns = new java.util.HashSet<>();

    for (MuleSoftFileParser.ErrorHandler handler : parsedFile.errorHandlers) {
      if (handler.element != null) {
        // Check response format consistency
        String format = detectResponseFormat(handler.element);
        if (format != null) {
          responseFormats.add(format);
        }

        // Check status code patterns
        String statusPattern = detectStatusCodePattern(handler.element);
        if (statusPattern != null) {
          statusCodePatterns.add(statusPattern);
        }
      }
    }

    // Report if multiple inconsistent formats are used
    if (responseFormats.size() > 1) {
      reportIssue(context, inputFile,
          "Inconsistent error response formats detected across error handlers. " +
          "Found formats: " + String.join(", ", responseFormats) + ". " +
          "Use a consistent error response format across all error handlers " +
          "for better API consistency and client integration.");
    }

    // Report if status codes are inconsistently set
    if (statusCodePatterns.size() > 1 && statusCodePatterns.contains("no-status-code")) {
      reportIssue(context, inputFile,
          "Inconsistent HTTP status code configuration across error handlers. " +
          "Some error handlers set status codes while others don't. " +
          "Ensure all error handlers consistently set appropriate HTTP status codes.");
    }
  }

  private String detectResponseFormat(org.w3c.dom.Element errorHandler) {
    // Check for set-payload with different content types
    org.w3c.dom.NodeList setPayloads = errorHandler.getElementsByTagName("set-payload");
    for (int i = 0; i < setPayloads.getLength(); i++) {
      org.w3c.dom.Element payload = (org.w3c.dom.Element) setPayloads.item(i);
      String value = payload.getAttribute("value");
      String textContent = payload.getTextContent();
      String content = value != null && !value.isEmpty() ? value : textContent;

      if (content != null) {
        if (content.contains("{") && content.contains("}")) {
          return "JSON";
        } else if (content.contains("<") && content.contains(">")) {
          return "XML";
        } else if (content.contains("error") || content.contains("message")) {
          return "Plain";
        }
      }
    }

    // Check for transform message (DataWeave)
    org.w3c.dom.NodeList transforms = errorHandler.getElementsByTagName("ee:transform");
    if (transforms.getLength() > 0) {
      for (int i = 0; i < transforms.getLength(); i++) {
        org.w3c.dom.Element transform = (org.w3c.dom.Element) transforms.item(i);
        String content = transform.getTextContent();
        if (content != null) {
          if (content.contains("application/json")) {
            return "JSON-DataWeave";
          } else if (content.contains("application/xml")) {
            return "XML-DataWeave";
          }
        }
      }
      return "DataWeave";
    }

    return null;
  }

  private String detectStatusCodePattern(org.w3c.dom.Element errorHandler) {
    // Check for set-variable or set-property with http status
    org.w3c.dom.NodeList setVariables = errorHandler.getElementsByTagName("set-variable");
    for (int i = 0; i < setVariables.getLength(); i++) {
      org.w3c.dom.Element var = (org.w3c.dom.Element) setVariables.item(i);
      String varName = var.getAttribute("variableName");
      if (varName != null && varName.toLowerCase().contains("status")) {
        return "has-status-code";
      }
    }

    // Check for http:error-response or similar status setting
    if (errorHandler.getElementsByTagName("http:error-response").getLength() > 0) {
      return "has-status-code";
    }

    // Check set-property for httpStatus
    org.w3c.dom.NodeList setProperties = errorHandler.getElementsByTagName("set-property");
    for (int i = 0; i < setProperties.getLength(); i++) {
      org.w3c.dom.Element prop = (org.w3c.dom.Element) setProperties.item(i);
      String propName = prop.getAttribute("propertyName");
      if (propName != null && propName.toLowerCase().contains("status")) {
        return "has-status-code";
      }
    }

    return "no-status-code";
  }
}
