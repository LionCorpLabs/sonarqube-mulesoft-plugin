package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Synchronous processing detected.
 */
@Rule(key = "MS072")
public class SynchronousProcessingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS072";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check for flows that should use async processing
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      checkForAsyncOpportunities(context, inputFile, flow, parsedFile);
    }

    // Check for missing async scopes
    checkForMissingAsyncScopes(context, inputFile, parsedFile);

    // Check for VM connector usage patterns
    checkVMConnectorUsage(context, inputFile, parsedFile);
  }

  private void checkForAsyncOpportunities(SensorContext context, InputFile inputFile,
                                         MuleSoftFileParser.MuleSoftFlow flow,
                                         MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check if flow has HTTP listener (request-response)
    boolean hasListener = hasHTTPListener(flow.element);

    if (hasListener) {
      // Check for long-running operations that don't affect response
      if (hasNonEssentialOperations(flow.element)) {
        reportIssue(context, inputFile,
            String.format("Flow '%s' contains operations that don't affect the response. " +
                "Consider moving logging, notifications, or analytics to async scope " +
                "to improve response time.",
                flow.name));
      }

      // Check for batch processing in request-response flow
      if (hasBatchProcessing(flow.element)) {
        reportIssue(context, inputFile,
            String.format("Flow '%s' is a request-response flow performing batch processing. " +
                "Consider using async processing with callback or polling pattern " +
                "to avoid blocking the client.",
                flow.name));
      }
    }

    // Check for fire-and-forget patterns
    if (hasFireAndForgetPattern(flow.element)) {
      reportIssue(context, inputFile,
          String.format("Flow '%s' appears to implement fire-and-forget pattern synchronously. " +
              "Consider using VM connector, JMS, or async scope for true asynchronous processing.",
              flow.name));
    }
  }

  private void checkForMissingAsyncScopes(SensorContext context, InputFile inputFile,
                                         MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check for patterns that should use async
    for (MuleSoftFileParser.LoggerComponent logger : parsedFile.loggers) {
      String level = logger.level;
      org.w3c.dom.Element parentFlow = findParentFlow(logger.element, parsedFile);

      if (parentFlow != null && hasHTTPListenerInElement(parentFlow)) {
        // Excessive logging in request-response flow
        if ("DEBUG".equalsIgnoreCase(level) || "TRACE".equalsIgnoreCase(level)) {
          if (!isInAsync(logger.element)) {
            reportIssue(context, inputFile,
                "Debug/trace logging in request-response flow should be in async scope " +
                "to avoid impacting response time.");
          }
        }
      }
    }
  }

  private void checkVMConnectorUsage(SensorContext context, InputFile inputFile,
                                    MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    org.w3c.dom.NodeList vmNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "publish");

    for (int i = 0; i < vmNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) vmNodes.item(i);
      String namespaceURI = element.getNamespaceURI();

      if (namespaceURI != null && namespaceURI.contains("vm")) {
        // Check if VM queue is used for async processing
        org.w3c.dom.Element parentFlow = findParentFlow(element, parsedFile);
        if (parentFlow != null) {
          String flowName = parentFlow.getAttribute("name");

          // This is good practice - VM for async
          // But check if there's a corresponding listener
          if (!hasVMListener(parsedFile.xmlDocument, flowName)) {
            reportIssue(context, inputFile,
                String.format("VM publish found in flow '%s' but no corresponding VM listener found. " +
                    "Ensure async processing is properly configured with VM queues.",
                    flowName));
          }
        }
      }
    }
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

  private boolean hasHTTPListenerInElement(org.w3c.dom.Element element) {
    return hasHTTPListener(element);
  }

  private boolean hasNonEssentialOperations(org.w3c.dom.Element flowElement) {
    // Check for operations that typically don't affect the response

    // Multiple logger statements
    org.w3c.dom.NodeList loggers = flowElement.getElementsByTagName("logger");
    if (loggers.getLength() > 3) {
      return true;
    }

    // Notifications or alerts
    org.w3c.dom.NodeList smtpNodes = flowElement.getElementsByTagNameNS("*", "send");
    for (int i = 0; i < smtpNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) smtpNodes.item(i);
      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI != null && (namespaceURI.contains("email") || namespaceURI.contains("smtp"))) {
        return true;
      }
    }

    // Analytics or tracking calls
    String flowContent = flowElement.getTextContent();
    if (flowContent.contains("analytics") || flowContent.contains("tracking") ||
        flowContent.contains("metrics")) {
      return true;
    }

    return false;
  }

  private boolean hasBatchProcessing(org.w3c.dom.Element flowElement) {
    org.w3c.dom.NodeList batchNodes = flowElement.getElementsByTagNameNS("*", "job");

    for (int i = 0; i < batchNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) batchNodes.item(i);
      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("batch")) {
        return true;
      }
    }

    // Check for foreach with many iterations (potential batch candidate)
    org.w3c.dom.NodeList foreachNodes = flowElement.getElementsByTagName("foreach");
    return foreachNodes.getLength() > 0;
  }

  private boolean hasFireAndForgetPattern(org.w3c.dom.Element flowElement) {
    // Check if flow has listener but doesn't set response payload
    if (!hasHTTPListener(flowElement)) {
      return false;
    }

    // Check if there's no explicit response set
    org.w3c.dom.NodeList setPayloadNodes = flowElement.getElementsByTagName("set-payload");
    org.w3c.dom.NodeList setResponseNodes = flowElement.getElementsByTagName("set-response");

    // If there are long-running operations but no response manipulation
    if (setPayloadNodes.getLength() == 0 && setResponseNodes.getLength() == 0) {
      return hasLongRunningOperations(flowElement);
    }

    return false;
  }

  private boolean hasLongRunningOperations(org.w3c.dom.Element flowElement) {
    // Check for batch jobs
    if (hasBatchProcessing(flowElement)) {
      return true;
    }

    // Check for multiple HTTP requests
    org.w3c.dom.NodeList httpNodes = flowElement.getElementsByTagNameNS("*", "request");
    int httpCount = 0;
    for (int i = 0; i < httpNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) httpNodes.item(i);
      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("http")) {
        httpCount++;
      }
    }

    return httpCount > 2;
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

  private org.w3c.dom.Element findParentFlow(org.w3c.dom.Element element, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      org.w3c.dom.Element parentElement = (org.w3c.dom.Element) parent;
      String tagName = parentElement.getTagName();
      if ("flow".equals(tagName) || "sub-flow".equals(tagName)) {
        return parentElement;
      }
      parent = parent.getParentNode();
    }
    return null;
  }

  private boolean hasVMListener(org.w3c.dom.Document doc, String flowContext) {
    org.w3c.dom.NodeList vmListeners = doc.getElementsByTagNameNS("*", "listener");

    for (int i = 0; i < vmListeners.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) vmListeners.item(i);
      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("vm")) {
        return true;
      }
    }
    return false;
  }
}
