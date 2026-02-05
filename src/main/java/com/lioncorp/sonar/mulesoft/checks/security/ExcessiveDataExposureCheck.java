package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Excessive data exposure in API.
 */
@Rule(key = "MS023")
public class ExcessiveDataExposureCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS023";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows for excessive data exposure in API responses
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (isAPIEndpoint(flow)) {
        checkForExcessiveDataExposure(context, inputFile, flow);
      }
    }
  }

  private boolean isAPIEndpoint(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    // Check if flow has HTTP listener (API endpoint)
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

  private void checkForExcessiveDataExposure(SensorContext context, InputFile inputFile, MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return;
    }

    String flowContent = flow.element.getTextContent();
    if (flowContent == null) {
      return;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for direct database query results being returned
    boolean hasDirectDBQuery = lowerContent.contains("db:select") || lowerContent.contains("database");
    boolean hasTransformation = lowerContent.contains("transform") ||
                               lowerContent.contains("set-payload") ||
                               lowerContent.contains("dataweave");
    boolean hasFieldFiltering = lowerContent.contains("filter") ||
                               lowerContent.contains("pluck") ||
                               lowerContent.contains("map");

    if (hasDirectDBQuery && !hasTransformation && !hasFieldFiltering) {
      reportIssue(context, inputFile,
          "Flow '" + flow.name + "' may expose excessive data. " +
          "Database query results are returned directly without filtering sensitive fields. " +
          "Use DataWeave transformations to return only necessary fields.");
    }

    // Check for sensitive field patterns in responses
    if (containsSensitiveFields(lowerContent) && !hasFieldFiltering) {
      reportIssue(context, inputFile,
          "Flow '" + flow.name + "' may expose sensitive data fields (password, ssn, token, etc.) in API responses. " +
          "Implement field filtering to exclude sensitive information from responses.");
    }
  }

  private boolean containsSensitiveFields(String content) {
    if (content == null) {
      return false;
    }

    String[] sensitivePatterns = {
        "password", "passwd", "pwd", "secret", "token", "apikey",
        "ssn", "credit_card", "card_number", "cvv", "pin",
        "private_key", "access_key", "auth"
    };

    for (String pattern : sensitivePatterns) {
      if (content.contains(pattern)) {
        return true;
      }
    }

    return false;
  }
}
