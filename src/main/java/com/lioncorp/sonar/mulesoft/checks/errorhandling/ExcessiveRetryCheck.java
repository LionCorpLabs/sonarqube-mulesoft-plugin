package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Excessive retry attempts.
 */
@Rule(key = "MS090")
public class ExcessiveRetryCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS090";
  }

  private static final int MAX_RETRY_COUNT = 5;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP request configurations for excessive retry attempts
    checkHttpRequestsForExcessiveRetries(context, inputFile, parsedFile);

    // Check until-successful scope for excessive retry attempts
    checkUntilSuccessfulForExcessiveRetries(context, inputFile, parsedFile);
  }

  private void checkHttpRequestsForExcessiveRetries(SensorContext context, InputFile inputFile,
                                                     MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if (httpConfig.element != null) {
        // Check for reconnection strategy
        org.w3c.dom.NodeList reconnections = httpConfig.element.getElementsByTagName("reconnect");
        for (int i = 0; i < reconnections.getLength(); i++) {
          org.w3c.dom.Element reconnect = (org.w3c.dom.Element) reconnections.item(i);
          String count = reconnect.getAttribute("count");
          if (count != null && !count.isEmpty()) {
            try {
              int retryCount = Integer.parseInt(count);
              if (retryCount > MAX_RETRY_COUNT) {
                reportIssue(context, inputFile,
                    "HTTP request has excessive retry attempts (" + retryCount + " retries). " +
                    "Consider using a maximum of " + MAX_RETRY_COUNT + " retries to avoid " +
                    "prolonged delays and resource exhaustion.");
              }
            } catch (NumberFormatException e) {
              // Ignore non-numeric values
            }
          }
        }

        // Check for reconnect-forever (unlimited retries)
        org.w3c.dom.NodeList reconnectForever = httpConfig.element.getElementsByTagName("reconnect-forever");
        if (reconnectForever.getLength() > 0) {
          reportIssue(context, inputFile,
              "HTTP request uses 'reconnect-forever' which retries indefinitely. " +
              "This can cause indefinite delays and resource exhaustion. " +
              "Use a bounded reconnection strategy instead.");
        }
      }
    }
  }

  private void checkUntilSuccessfulForExcessiveRetries(SensorContext context, InputFile inputFile,
                                                        MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument != null) {
      org.w3c.dom.NodeList untilSuccessful = parsedFile.xmlDocument.getElementsByTagName("until-successful");
      for (int i = 0; i < untilSuccessful.getLength(); i++) {
        org.w3c.dom.Element element = (org.w3c.dom.Element) untilSuccessful.item(i);
        String maxRetries = element.getAttribute("maxRetries");
        if (maxRetries != null && !maxRetries.isEmpty()) {
          try {
            int retryCount = Integer.parseInt(maxRetries);
            if (retryCount > MAX_RETRY_COUNT) {
              reportIssue(context, inputFile,
                  "Until-successful scope has excessive retry attempts (" + retryCount + " retries). " +
                  "Consider using a maximum of " + MAX_RETRY_COUNT + " retries to avoid " +
                  "prolonged processing and resource exhaustion.");
            }
          } catch (NumberFormatException e) {
            // Ignore non-numeric values
          }
        }
      }
    }
  }
}
