package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Admin endpoint without protection.
 */
@Rule(key = "MS014")
public class ExposedAdminEndpointCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS014";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows for exposed admin endpoints without protection
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (isAdminEndpoint(flow) && !hasAuthenticationOrProtection(flow)) {
        reportIssue(context, inputFile,
            "Admin endpoint '" + flow.name + "' is exposed without authentication or protection. " +
            "Implement authentication, authorization, and IP whitelisting for administrative endpoints.");
      }
    }

    // Check HTTP configurations for admin paths
    for (MuleSoftFileParser.HttpConfiguration config : parsedFile.httpConfigurations) {
      if (config.element != null) {
        String path = config.element.getAttribute("path");
        if (path != null && isAdminPath(path) && !config.hasAuthentication) {
          reportIssue(context, inputFile,
              "Admin path '" + path + "' exposed without authentication. Secure administrative endpoints with proper authentication and authorization.");
        }
      }
    }
  }

  private boolean isAdminEndpoint(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.name == null) {
      return false;
    }

    String lowerName = flow.name.toLowerCase();
    return lowerName.contains("admin") || lowerName.contains("manage") ||
           lowerName.contains("console") || lowerName.contains("dashboard") ||
           lowerName.contains("config") || lowerName.contains("settings");
  }

  private boolean isAdminPath(String path) {
    if (path == null) {
      return false;
    }

    String lowerPath = path.toLowerCase();
    return lowerPath.contains("/admin") || lowerPath.contains("/manage") ||
           lowerPath.contains("/console") || lowerPath.contains("/dashboard") ||
           lowerPath.contains("/config") || lowerPath.contains("/settings") ||
           lowerPath.contains("/actuator") || lowerPath.contains("/health");
  }

  private boolean hasAuthenticationOrProtection(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    org.w3c.dom.Element flowElement = flow.element;
    String flowContent = flowElement.getTextContent();

    if (flowContent == null) {
      return false;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for authentication/authorization components
    return lowerContent.contains("authentication") ||
           lowerContent.contains("authorization") ||
           lowerContent.contains("security") ||
           lowerContent.contains("oauth") ||
           lowerContent.contains("basic-auth") ||
           lowerContent.contains("jwt") ||
           lowerContent.contains("api-key") ||
           (lowerContent.contains("ip") && lowerContent.contains("filter"));
  }
}
