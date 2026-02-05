package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Unbounded scatter-gather.
 */
@Rule(key = "MS077")
public class UnboundedScatterGatherCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS077";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Find all scatter-gather elements
    org.w3c.dom.NodeList scatterGatherNodes = parsedFile.xmlDocument.getElementsByTagName("scatter-gather");

    for (int i = 0; i < scatterGatherNodes.getLength(); i++) {
      org.w3c.dom.Element scatterGatherElement = (org.w3c.dom.Element) scatterGatherNodes.item(i);

      // Check if timeout is configured
      if (!hasTimeout(scatterGatherElement)) {
        reportIssue(context, inputFile,
            "Scatter-gather without timeout detected. This can cause indefinite blocking if " +
            "one of the routes hangs. Add timeout attribute to ensure timely failure.");
      }

      // Check if max concurrency is configured
      if (!hasMaxConcurrency(scatterGatherElement)) {
        reportIssue(context, inputFile,
            "Scatter-gather without maxConcurrency configured. This can lead to resource " +
            "exhaustion with many parallel routes. Consider setting maxConcurrency to limit " +
            "parallel execution.");
      }

      // Check number of routes
      int routeCount = countRoutes(scatterGatherElement);
      if (routeCount > 5) {
        reportIssue(context, inputFile,
            String.format("Scatter-gather has %d routes. Too many parallel routes can cause " +
                "performance issues and resource contention. Consider reducing routes or " +
                "using sequential processing for some operations.", routeCount));
      }

      // Check if routes contain blocking operations
      if (hasBlockingOperations(scatterGatherElement) && !hasTimeout(scatterGatherElement)) {
        reportIssue(context, inputFile,
            "Scatter-gather contains blocking operations (HTTP requests, database queries) " +
            "without timeout. This increases the risk of indefinite blocking. Add timeout configuration.");
      }
    }
  }

  private boolean hasTimeout(org.w3c.dom.Element scatterGatherElement) {
    // Check for timeout attribute
    if (scatterGatherElement.hasAttribute("timeout")) {
      String timeout = scatterGatherElement.getAttribute("timeout");
      return timeout != null && !timeout.isEmpty();
    }

    // Check for timeout child element
    org.w3c.dom.NodeList timeoutNodes = scatterGatherElement.getElementsByTagName("timeout");
    return timeoutNodes.getLength() > 0;
  }

  private boolean hasMaxConcurrency(org.w3c.dom.Element scatterGatherElement) {
    // Check for maxConcurrency attribute
    if (scatterGatherElement.hasAttribute("maxConcurrency")) {
      String maxConcurrency = scatterGatherElement.getAttribute("maxConcurrency");
      return maxConcurrency != null && !maxConcurrency.isEmpty();
    }

    // Check for max-concurrency attribute (alternative naming)
    if (scatterGatherElement.hasAttribute("max-concurrency")) {
      String maxConcurrency = scatterGatherElement.getAttribute("max-concurrency");
      return maxConcurrency != null && !maxConcurrency.isEmpty();
    }

    return false;
  }

  private int countRoutes(org.w3c.dom.Element scatterGatherElement) {
    // Count route elements
    org.w3c.dom.NodeList routeNodes = scatterGatherElement.getElementsByTagName("route");

    // Also count direct child elements that act as routes
    if (routeNodes.getLength() == 0) {
      // In some versions, routes might be direct children
      int routeCount = 0;
      org.w3c.dom.NodeList children = scatterGatherElement.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
          routeCount++;
        }
      }
      return routeCount;
    }

    return routeNodes.getLength();
  }

  private boolean hasBlockingOperations(org.w3c.dom.Element scatterGatherElement) {
    // Check for HTTP requests
    org.w3c.dom.NodeList httpNodes = scatterGatherElement.getElementsByTagNameNS("*", "request");
    for (int i = 0; i < httpNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) httpNodes.item(i);
      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("http")) {
        return true;
      }
    }

    // Check for database operations
    String[] dbOperations = {"select", "insert", "update", "delete"};
    for (String operation : dbOperations) {
      org.w3c.dom.NodeList dbNodes = scatterGatherElement.getElementsByTagNameNS("*", operation);
      for (int i = 0; i < dbNodes.getLength(); i++) {
        org.w3c.dom.Element element = (org.w3c.dom.Element) dbNodes.item(i);
        String namespaceURI = element.getNamespaceURI();
        if (namespaceURI != null && (namespaceURI.contains("database") || namespaceURI.contains("/db"))) {
          return true;
        }
      }
    }

    return false;
  }
}
