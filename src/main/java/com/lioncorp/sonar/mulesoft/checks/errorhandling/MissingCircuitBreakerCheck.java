package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing circuit breaker.
 */
@Rule(key = "MS091")
public class MissingCircuitBreakerCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS091";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP requests for circuit breaker pattern
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request".equals(httpConfig.type) && !hasCircuitBreaker(httpConfig.element)) {
        reportIssue(context, inputFile,
            "External HTTP request does not have a circuit breaker configured. " +
            "Circuit breakers protect against cascading failures when external services " +
            "are unavailable. Consider wrapping external calls with circuit breaker logic " +
            "or using until-successful with appropriate error handling.");
      }
    }

    // Check database operations for circuit breaker pattern
    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      if (isDbOperation(dbConnector.type) && !hasCircuitBreaker(dbConnector.element)) {
        reportIssue(context, inputFile,
            "Database operation does not have a circuit breaker or timeout configured. " +
            "Circuit breakers help prevent resource exhaustion when database connections fail. " +
            "Configure appropriate timeout and reconnection strategies.");
      }
    }
  }

  private boolean hasCircuitBreaker(org.w3c.dom.Element element) {
    if (element == null) {
      return false;
    }

    // Check if element is wrapped in until-successful
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      String nodeName = parent.getNodeName();
      if ("until-successful".equals(nodeName)) {
        return true;
      }
      // Check for custom circuit breaker component
      if (nodeName.contains("circuit-breaker") || nodeName.contains("resilience")) {
        return true;
      }
      parent = parent.getParentNode();
    }

    // Check for reconnection strategy which provides some circuit breaker behavior
    org.w3c.dom.NodeList reconnections = element.getElementsByTagName("reconnect");
    if (reconnections.getLength() > 0) {
      return true;
    }

    // Check for timeout configuration (minimal protection)
    String timeout = element.getAttribute("timeout");
    String responseTimeout = element.getAttribute("responseTimeout");
    if ((timeout != null && !timeout.isEmpty()) ||
        (responseTimeout != null && !responseTimeout.isEmpty())) {
      // Has timeout but not full circuit breaker - still flag it
      return false;
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
}
