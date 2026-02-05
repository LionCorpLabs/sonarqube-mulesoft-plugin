package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing timeout.
 */
@Rule(key = "MS094")
public class MissingTimeoutConfigurationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS094";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP requests for timeout configuration
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request".equals(httpConfig.type) && !hasTimeoutConfiguration(httpConfig.element)) {
        reportIssue(context, inputFile,
            "HTTP request does not have a timeout configured. " +
            "Operations without timeouts can hang indefinitely and cause resource exhaustion. " +
            "Configure responseTimeout or use a timeout wrapper to prevent indefinite blocking.");
      }
    }

    // Check database operations for timeout
    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      if (isDbOperation(dbConnector.type) && !hasTimeoutConfiguration(dbConnector.element)) {
        reportIssue(context, inputFile,
            "Database operation does not have a timeout configured. " +
            "Configure queryTimeout to prevent long-running queries from blocking resources.");
      }
    }

    // Check until-successful for timeout
    checkUntilSuccessfulTimeout(context, inputFile, parsedFile);

    // Check other operations that should have timeouts
    checkExternalOperationsForTimeout(context, inputFile, parsedFile);
  }

  private boolean hasTimeoutConfiguration(org.w3c.dom.Element element) {
    if (element == null) {
      return false;
    }

    // Check common timeout attributes
    String[] timeoutAttrs = {"timeout", "responseTimeout", "queryTimeout",
                              "connectionTimeout", "socketTimeout"};

    for (String attr : timeoutAttrs) {
      String value = element.getAttribute(attr);
      if (value != null && !value.isEmpty() && !"0".equals(value)) {
        return true;
      }
    }

    // Check for timeout configuration in child elements
    org.w3c.dom.NodeList timeoutNodes = element.getElementsByTagName("timeout");
    if (timeoutNodes.getLength() > 0) {
      return true;
    }

    return false;
  }

  private boolean isDbOperation(String type) {
    return "select".equals(type) ||
           "insert".equals(type) ||
           "update".equals(type) ||
           "delete".equals(type) ||
           "bulk-insert".equals(type);
  }

  private void checkUntilSuccessfulTimeout(SensorContext context, InputFile inputFile,
                                            MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    org.w3c.dom.NodeList untilSuccessful = parsedFile.xmlDocument.getElementsByTagName("until-successful");
    for (int i = 0; i < untilSuccessful.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) untilSuccessful.item(i);
      String maxRetries = element.getAttribute("maxRetries");
      String millisBetweenRetries = element.getAttribute("millisBetweenRetries");

      // If retries are configured but no overall timeout, warn
      if ((maxRetries != null && !maxRetries.isEmpty()) &&
          (millisBetweenRetries != null && !millisBetweenRetries.isEmpty())) {
        // Calculate potential total time
        try {
          int retries = Integer.parseInt(maxRetries);
          int interval = Integer.parseInt(millisBetweenRetries);
          long totalTime = (long) retries * interval;

          // If total time exceeds 30 seconds, recommend explicit timeout
          if (totalTime > 30000) {
            reportIssue(context, inputFile,
                "Until-successful configuration may result in very long wait times (" +
                (totalTime / 1000) + " seconds). Consider adding an explicit timeout limit.");
          }
        } catch (NumberFormatException e) {
          // Ignore parsing errors
        }
      }
    }
  }

  private void checkExternalOperationsForTimeout(SensorContext context, InputFile inputFile,
                                                  MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check for operations that typically need timeouts
    String[] operationsTags = {"ftp:write", "sftp:write", "jms:publish", "jms:consume"};

    for (String tag : operationsTags) {
      String[] parts = tag.split(":");
      if (parts.length == 2) {
        org.w3c.dom.NodeList nodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", parts[1]);
        for (int i = 0; i < nodes.getLength(); i++) {
          org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);
          if (element.getNamespaceURI() != null &&
              element.getNamespaceURI().contains(parts[0]) &&
              !hasTimeoutConfiguration(element)) {
            reportIssue(context, inputFile,
                "Operation " + tag + " does not have a timeout configured. " +
                "Configure timeout to prevent indefinite blocking.");
          }
        }
      }
    }
  }
}
