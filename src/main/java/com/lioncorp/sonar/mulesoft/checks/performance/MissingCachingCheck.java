package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing caching.
 */
@Rule(key = "MS073")
public class MissingCachingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS073";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check for repeated HTTP requests
    checkRepeatedHTTPRequests(context, inputFile, parsedFile);

    // Check for repeated database queries
    checkRepeatedDatabaseQueries(context, inputFile, parsedFile);

    // Check for repeated flow-ref calls
    checkRepeatedFlowRefs(context, inputFile, parsedFile);

    // Check for existence of cache scope
    checkForCacheUsage(context, inputFile, parsedFile);
  }

  private void checkRepeatedHTTPRequests(SensorContext context, InputFile inputFile,
                                        MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    java.util.Map<String, java.util.List<org.w3c.dom.Element>> requestsByUrl = new java.util.HashMap<>();

    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request".equals(httpConfig.type)) {
        String url = buildUrl(httpConfig);
        String method = httpConfig.element.getAttribute("method");

        // Only cache GET requests
        if ("GET".equalsIgnoreCase(method) || method.isEmpty()) {
          String key = url + "|" + method;
          requestsByUrl.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(httpConfig.element);
        }
      }
    }

    // Report repeated requests
    for (java.util.Map.Entry<String, java.util.List<org.w3c.dom.Element>> entry : requestsByUrl.entrySet()) {
      if (entry.getValue().size() > 1) {
        String url = entry.getKey().split("\\|")[0];
        if (!isInCacheScope(entry.getValue().get(0))) {
          reportIssue(context, inputFile,
              String.format("HTTP GET request to '%s' is called multiple times. " +
                  "Consider using cache scope to cache the response and improve performance.",
                  url.isEmpty() ? "endpoint" : url));
        }
      }
    }
  }

  private void checkRepeatedDatabaseQueries(SensorContext context, InputFile inputFile,
                                           MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    java.util.Map<String, java.util.List<org.w3c.dom.Element>> queriesBySignature = new java.util.HashMap<>();

    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      if ("select".equals(dbConnector.type)) {
        String query = normalizeQuery(dbConnector.element.getTextContent());

        if (!query.isEmpty()) {
          queriesBySignature.computeIfAbsent(query, k -> new java.util.ArrayList<>()).add(dbConnector.element);
        }
      }
    }

    // Report repeated queries
    for (java.util.Map.Entry<String, java.util.List<org.w3c.dom.Element>> entry : queriesBySignature.entrySet()) {
      if (entry.getValue().size() > 1) {
        if (!isInCacheScope(entry.getValue().get(0))) {
          reportIssue(context, inputFile,
              "Similar database SELECT queries are executed multiple times. " +
              "Consider using cache scope or storing results in a variable to improve performance.");
        }
      }
    }
  }

  private void checkRepeatedFlowRefs(SensorContext context, InputFile inputFile,
                                    MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    java.util.Map<String, Integer> flowRefCounts = new java.util.HashMap<>();

    for (MuleSoftFileParser.FlowReference flowRef : parsedFile.flowReferences) {
      String flowName = flowRef.flowName;
      if (flowName != null && !flowName.isEmpty()) {
        flowRefCounts.put(flowName, flowRefCounts.getOrDefault(flowName, 0) + 1);
      }
    }

    // Check flows that are called multiple times
    for (java.util.Map.Entry<String, Integer> entry : flowRefCounts.entrySet()) {
      if (entry.getValue() > 2) {
        // Find the referenced flow
        MuleSoftFileParser.MuleSoftFlow referencedFlow = findFlow(parsedFile, entry.getKey());

        if (referencedFlow != null && containsExpensiveOperation(referencedFlow.element)) {
          reportIssue(context, inputFile,
              String.format("Flow '%s' is called %d times and contains expensive operations. " +
                  "Consider caching the results if the flow output is deterministic.",
                  entry.getKey(), entry.getValue()));
        }
      }
    }
  }

  private void checkForCacheUsage(SensorContext context, InputFile inputFile,
                                 MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check if cache scope is configured
    org.w3c.dom.NodeList cacheNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "cache");
    org.w3c.dom.NodeList objectStoreNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "object-store");

    boolean hasCaching = cacheNodes.getLength() > 0 || objectStoreNodes.getLength() > 0;

    // If no caching is configured but there are multiple HTTP requests or DB queries
    if (!hasCaching) {
      int httpRequestCount = (int) parsedFile.httpConfigurations.stream()
          .filter(h -> "request".equals(h.type)).count();
      int dbQueryCount = (int) parsedFile.databaseConnectors.stream()
          .filter(d -> "select".equals(d.type)).count();

      if (httpRequestCount > 3 || dbQueryCount > 3) {
        reportIssue(context, inputFile,
            "Multiple HTTP requests or database queries detected without caching. " +
            "Consider implementing cache scope or object store for frequently accessed data.");
      }
    }
  }

  private String buildUrl(MuleSoftFileParser.HttpConfiguration httpConfig) {
    String host = httpConfig.host != null ? httpConfig.host : "";
    String port = httpConfig.port != null ? httpConfig.port : "";
    String path = httpConfig.element.getAttribute("path");

    return host + (port.isEmpty() ? "" : ":" + port) + (path != null ? path : "");
  }

  private String normalizeQuery(String query) {
    if (query == null) {
      return "";
    }
    // Remove extra whitespace and normalize
    return query.replaceAll("\\s+", " ").trim().toLowerCase();
  }

  private boolean isInCacheScope(org.w3c.dom.Element element) {
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      org.w3c.dom.Element parentElement = (org.w3c.dom.Element) parent;
      String localName = parentElement.getLocalName();

      if ("cache".equals(localName) || "cachingStrategy".equals(localName)) {
        return true;
      }
      parent = parent.getParentNode();
    }
    return false;
  }

  private MuleSoftFileParser.MuleSoftFlow findFlow(MuleSoftFileParser.ParsedMuleSoftFile parsedFile, String flowName) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flowName.equals(flow.name)) {
        return flow;
      }
    }
    return null;
  }

  private boolean containsExpensiveOperation(org.w3c.dom.Element element) {
    // Check for database operations
    org.w3c.dom.NodeList dbNodes = element.getElementsByTagNameNS("*", "*");
    for (int i = 0; i < dbNodes.getLength(); i++) {
      org.w3c.dom.Element node = (org.w3c.dom.Element) dbNodes.item(i);
      String namespace = node.getNamespaceURI();
      if (namespace != null && (namespace.contains("database") || namespace.contains("/db"))) {
        return true;
      }
      if (namespace != null && namespace.contains("http")) {
        return true;
      }
    }
    return false;
  }
}
