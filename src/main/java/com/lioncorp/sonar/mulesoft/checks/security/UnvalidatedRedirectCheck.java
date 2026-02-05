package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Unvalidated redirect detected.
 */
@Rule(key = "MS015")
public class UnvalidatedRedirectCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS015";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows for unvalidated redirects
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.element != null) {
        checkFlowForUnvalidatedRedirects(context, inputFile, flow);
      }
    }

    // Check raw content for redirect patterns
    if (parsedFile.rawContent != null) {
      checkContentForUnvalidatedRedirects(context, inputFile, parsedFile.rawContent);
    }
  }

  private void checkFlowForUnvalidatedRedirects(SensorContext context, InputFile inputFile, MuleSoftFileParser.MuleSoftFlow flow) {
    org.w3c.dom.Element flowElement = flow.element;
    String flowContent = flowElement.getTextContent();

    if (flowContent == null) {
      return;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for redirect operations with dynamic user input
    if ((lowerContent.contains("redirect") || lowerContent.contains("location") || lowerContent.contains("forward")) &&
        (lowerContent.contains("payload") || lowerContent.contains("attributes.queryparams") ||
         lowerContent.contains("vars.") || lowerContent.contains("message.inboundproperties"))) {

      // Check if there's validation
      if (!hasRedirectValidation(flowContent)) {
        reportIssue(context, inputFile,
            "Flow '" + flow.name + "' performs redirect using user input without validation. " +
            "Validate redirect URLs against a whitelist to prevent open redirect vulnerabilities.");
      }
    }
  }

  private void checkContentForUnvalidatedRedirects(SensorContext context, InputFile inputFile, String content) {
    // Look for set-property or set-variable with Location header
    if (content.contains("Location") && content.contains("propertyName=\"Location\"")) {
      if (content.contains("payload") || content.contains("attributes.queryParams") || content.contains("vars.")) {
        if (!hasRedirectValidation(content)) {
          reportIssue(context, inputFile,
              "Setting Location header with user input without validation. Implement whitelist validation for redirect URLs.");
        }
      }
    }
  }

  private boolean hasRedirectValidation(String content) {
    if (content == null) {
      return false;
    }

    String lowerContent = content.toLowerCase();

    // Check for validation patterns
    return lowerContent.contains("whitelist") ||
           lowerContent.contains("allowed") ||
           lowerContent.contains("validate") ||
           lowerContent.contains("matches") ||
           (lowerContent.contains("contains") && lowerContent.contains("http")) ||
           lowerContent.contains("startswith");
  }
}
