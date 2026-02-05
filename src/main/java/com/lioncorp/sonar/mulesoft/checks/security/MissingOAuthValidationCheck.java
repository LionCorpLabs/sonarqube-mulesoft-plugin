package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Missing OAuth/JWT validation.
 */
@Rule(key = "MS019")
public class MissingOAuthValidationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS019";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP configurations for OAuth endpoints without proper validation
    for (MuleSoftFileParser.HttpConfiguration config : parsedFile.httpConfigurations) {
      if (isOAuthEndpoint(config)) {
        checkOAuthValidation(context, inputFile, config);
      }
    }

    // Check flows for OAuth flows without token validation
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (isOAuthFlow(flow)) {
        checkOAuthFlowValidation(context, inputFile, flow);
      }
    }
  }

  private boolean isOAuthEndpoint(MuleSoftFileParser.HttpConfiguration config) {
    if (config == null || config.element == null) {
      return false;
    }

    Element element = config.element;
    String id = element.getAttribute("id");
    String name = element.getAttribute("name");
    String path = element.getAttribute("path");
    String configRef = element.getAttribute("config-ref");

    String combined = (id != null ? id : "") + (name != null ? name : "") +
                      (path != null ? path : "") + (configRef != null ? configRef : "");
    String lowerCombined = combined.toLowerCase();

    return lowerCombined.contains("oauth") || lowerCombined.contains("token") ||
           lowerCombined.contains("auth") || lowerCombined.contains("authorize");
  }

  private void checkOAuthValidation(SensorContext context, InputFile inputFile, MuleSoftFileParser.HttpConfiguration config) {
    Element element = config.element;

    // Check if authentication is configured
    if (!config.hasAuthentication) {
      NodeList authElements = element.getElementsByTagName("authentication");
      if (authElements.getLength() == 0) {
        reportIssue(context, inputFile,
            "OAuth endpoint lacks authentication configuration. " +
            "Implement OAuth token validation or JWT verification to secure this endpoint.");
        return;
      }
    }

    // Check if TLS/SSL is enabled for OAuth endpoints
    if (!config.hasTLS) {
      reportIssue(context, inputFile,
          "OAuth endpoint does not use HTTPS/TLS. " +
          "OAuth tokens must be transmitted over encrypted connections to prevent interception.");
    }
  }

  private boolean isOAuthFlow(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow == null || flow.element == null) {
      return false;
    }

    String flowName = flow.name;
    if (flowName == null) {
      return false;
    }

    String lowerName = flowName.toLowerCase();
    return lowerName.contains("oauth") || lowerName.contains("token") ||
           lowerName.contains("authorize") || lowerName.contains("jwt");
  }

  private void checkOAuthFlowValidation(SensorContext context, InputFile inputFile, MuleSoftFileParser.MuleSoftFlow flow) {
    Element flowElement = flow.element;

    // Check for token validation components
    boolean hasTokenValidation = hasTokenValidationComponent(flowElement);
    if (!hasTokenValidation) {
      reportIssue(context, inputFile,
          "OAuth/JWT flow '" + flow.name + "' lacks token validation. " +
          "Implement token verification, signature validation, and expiration checks.");
    }
  }

  private boolean hasTokenValidationComponent(Element flowElement) {
    // Check for JWT validation
    NodeList jwtNodes = flowElement.getElementsByTagNameNS("*", "validate-jwt");
    if (jwtNodes.getLength() > 0) {
      return true;
    }

    // Check for custom token validation
    NodeList components = flowElement.getElementsByTagName("component");
    for (int i = 0; i < components.getLength(); i++) {
      Element component = (Element) components.item(i);
      String className = component.getAttribute("class");
      if (className != null && (className.toLowerCase().contains("token") ||
                                className.toLowerCase().contains("jwt") ||
                                className.toLowerCase().contains("validat"))) {
        return true;
      }
    }

    // Check for OAuth provider validation
    NodeList authElements = flowElement.getElementsByTagName("authentication");
    if (authElements.getLength() > 0) {
      return true;
    }

    // Check for choice router with token validation logic
    NodeList choices = flowElement.getElementsByTagName("choice");
    for (int i = 0; i < choices.getLength(); i++) {
      Element choice = (Element) choices.item(i);
      String choiceContent = choice.getTextContent();
      if (choiceContent != null && (choiceContent.toLowerCase().contains("token") ||
                                    choiceContent.toLowerCase().contains("jwt"))) {
        return true;
      }
    }

    return false;
  }
}
