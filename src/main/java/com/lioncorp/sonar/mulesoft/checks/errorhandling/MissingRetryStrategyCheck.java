package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing retry strategy.
 */
@Rule(key = "MS089")
public class MissingRetryStrategyCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS089";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP requests for retry strategy
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request".equals(httpConfig.type) && !hasRetryStrategy(httpConfig.element)) {
        reportIssue(context, inputFile,
            "External HTTP request does not have a retry strategy configured. " +
            "Transient failures (network issues, timeouts) can cause operations to fail. " +
            "Configure a reconnection strategy or wrap the call in until-successful " +
            "to improve reliability.");
      }
    }

    // Check database operations for retry strategy
    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      if (isDbOperation(dbConnector.type) && !hasRetryStrategy(dbConnector.element)) {
        reportIssue(context, inputFile,
            "Database operation does not have a retry strategy configured. " +
            "Transient database connection issues can cause operations to fail. " +
            "Configure a reconnection strategy to improve reliability.");
      }
    }

    // Check other external connectors (FTP, SFTP, JMS, etc.)
    checkExternalConnectorsForRetry(context, inputFile, parsedFile);
  }

  private boolean hasRetryStrategy(org.w3c.dom.Element element) {
    if (element == null) {
      return false;
    }

    // Check for reconnection strategy
    org.w3c.dom.NodeList reconnections = element.getElementsByTagName("reconnect");
    if (reconnections.getLength() > 0) {
      return true;
    }

    org.w3c.dom.NodeList reconnectForever = element.getElementsByTagName("reconnect-forever");
    if (reconnectForever.getLength() > 0) {
      return true;
    }

    // Check if wrapped in until-successful
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      if ("until-successful".equals(parent.getNodeName())) {
        return true;
      }
      parent = parent.getParentNode();
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

  private void checkExternalConnectorsForRetry(SensorContext context, InputFile inputFile,
                                                MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check for external connectors that should have retry
    String[] externalConnectors = {"ftp:write", "sftp:write", "jms:publish", "jms:consume",
                                    "vm:publish", "amqp:publish", "file:write"};

    for (String connector : externalConnectors) {
      String[] parts = connector.split(":");
      if (parts.length == 2) {
        org.w3c.dom.NodeList nodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", parts[1]);
        for (int i = 0; i < nodes.getLength(); i++) {
          org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);
          if (element.getNamespaceURI() != null &&
              element.getNamespaceURI().contains(parts[0]) &&
              !hasRetryStrategy(element)) {
            reportIssue(context, inputFile,
                "External connector (" + connector + ") does not have a retry strategy. " +
                "Configure reconnection strategy or until-successful for better resilience.");
          }
        }
      }
    }
  }
}
