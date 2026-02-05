package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Synchronous API call.
 */
@Rule(key = "MS082")
public class SynchronousAPICallCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS082";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check for sequential HTTP requests that could be parallelized
    checkSequentialHTTPRequests(context, inputFile, parsedFile);

    // Check for HTTP requests without timeout configuration
    checkHTTPRequestTimeouts(context, inputFile, parsedFile);

    // Check for blocking operations in request-response flows
    checkBlockingOperationsInFlow(context, inputFile, parsedFile);
  }

  private void checkSequentialHTTPRequests(SensorContext context, InputFile inputFile,
                                          MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      java.util.List<org.w3c.dom.Element> httpRequests = findSequentialHTTPRequests(flow.element);

      if (httpRequests.size() > 1) {
        // Check if these requests are independent (could be parallelized)
        if (!isInScatterGather(httpRequests.get(0)) && !isInAsync(httpRequests.get(0))) {
          reportIssue(context, inputFile,
              String.format("Flow '%s' contains %d sequential HTTP requests. " +
                  "Consider using scatter-gather or async scope to parallelize independent API calls " +
                  "and improve performance.",
                  flow.name, httpRequests.size()));
        }
      }
    }
  }

  private void checkHTTPRequestTimeouts(SensorContext context, InputFile inputFile,
                                       MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request".equals(httpConfig.type) || "request-config".equals(httpConfig.type)) {
        if (!hasTimeoutConfiguration(httpConfig.element)) {
          String configName = httpConfig.element.getAttribute("name");
          String path = httpConfig.element.getAttribute("path");

          reportIssue(context, inputFile,
              String.format("HTTP request '%s' is missing timeout configuration. " +
                  "Synchronous API calls without timeouts can cause flow to hang indefinitely. " +
                  "Add responseTimeout and connection timeout.",
                  !path.isEmpty() ? path : (!configName.isEmpty() ? configName : "unnamed")));
        }
      }
    }
  }

  private void checkBlockingOperationsInFlow(SensorContext context, InputFile inputFile,
                                            MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows that have HTTP listeners (request-response pattern)
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (hasHTTPListener(flow.element)) {
        int blockingCallCount = countBlockingCalls(flow.element);

        if (blockingCallCount > 2) {
          reportIssue(context, inputFile,
              String.format("Flow '%s' is a request-response flow with %d blocking API calls. " +
                  "This can cause poor response times. Consider using async processing, " +
                  "scatter-gather for parallel calls, or moving long-running operations to async flows.",
                  flow.name, blockingCallCount));
        }
      }
    }
  }

  private java.util.List<org.w3c.dom.Element> findSequentialHTTPRequests(org.w3c.dom.Element flowElement) {
    java.util.List<org.w3c.dom.Element> requests = new java.util.ArrayList<>();

    org.w3c.dom.NodeList children = flowElement.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        org.w3c.dom.Element element = (org.w3c.dom.Element) child;

        if (isHTTPRequest(element)) {
          requests.add(element);
        }
      }
    }

    return requests;
  }

  private boolean isHTTPRequest(org.w3c.dom.Element element) {
    String localName = element.getLocalName();
    String namespaceURI = element.getNamespaceURI();

    return "request".equals(localName) &&
           namespaceURI != null &&
           namespaceURI.contains("http");
  }

  private boolean isInScatterGather(org.w3c.dom.Element element) {
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      org.w3c.dom.Element parentElement = (org.w3c.dom.Element) parent;
      if ("scatter-gather".equals(parentElement.getLocalName())) {
        return true;
      }
      parent = parent.getParentNode();
    }
    return false;
  }

  private boolean isInAsync(org.w3c.dom.Element element) {
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      org.w3c.dom.Element parentElement = (org.w3c.dom.Element) parent;
      String localName = parentElement.getLocalName();
      if ("async".equals(localName) || "async-scope".equals(localName)) {
        return true;
      }
      parent = parent.getParentNode();
    }
    return false;
  }

  private boolean hasTimeoutConfiguration(org.w3c.dom.Element element) {
    // Check for responseTimeout attribute
    if (element.hasAttribute("responseTimeout")) {
      return true;
    }

    // Check for timeout child elements
    org.w3c.dom.NodeList timeoutNodes = element.getElementsByTagName("response-timeout");
    if (timeoutNodes.getLength() > 0) {
      return true;
    }

    // Check in parent configuration
    String configRef = element.getAttribute("config-ref");
    // If config-ref is present, assume timeout might be configured there
    return !configRef.isEmpty();
  }

  private boolean hasHTTPListener(org.w3c.dom.Element flowElement) {
    org.w3c.dom.NodeList listeners = flowElement.getElementsByTagNameNS("*", "listener");

    for (int i = 0; i < listeners.getLength(); i++) {
      org.w3c.dom.Element listener = (org.w3c.dom.Element) listeners.item(i);
      String namespaceURI = listener.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("http")) {
        return true;
      }
    }
    return false;
  }

  private int countBlockingCalls(org.w3c.dom.Element flowElement) {
    int count = 0;

    // Count HTTP requests
    org.w3c.dom.NodeList httpNodes = flowElement.getElementsByTagNameNS("*", "request");
    for (int i = 0; i < httpNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) httpNodes.item(i);
      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("http")) {
        // Don't count if inside async or scatter-gather
        if (!isInAsync(element) && !isInScatterGather(element)) {
          count++;
        }
      }
    }

    // Count database calls
    org.w3c.dom.NodeList dbNodes = flowElement.getElementsByTagNameNS("*", "*");
    for (int i = 0; i < dbNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) dbNodes.item(i);
      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI != null && (namespaceURI.contains("database") || namespaceURI.contains("/db"))) {
        String localName = element.getLocalName();
        if ("select".equals(localName) || "insert".equals(localName) ||
            "update".equals(localName) || "delete".equals(localName)) {
          if (!isInAsync(element)) {
            count++;
          }
        }
      }
    }

    return count;
  }
}
