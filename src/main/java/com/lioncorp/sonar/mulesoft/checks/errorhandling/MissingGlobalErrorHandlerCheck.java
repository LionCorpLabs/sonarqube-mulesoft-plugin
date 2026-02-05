package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing global error handler.
 */
@Rule(key = "MS085")
public class MissingGlobalErrorHandlerCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS085";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check if there's at least one global error handler (configuration element)
    boolean hasGlobalErrorHandler = false;

    if (parsedFile.xmlDocument != null) {
      // Look for configuration element with error-handler
      org.w3c.dom.NodeList configurations = parsedFile.xmlDocument.getElementsByTagName("configuration");
      for (int i = 0; i < configurations.getLength(); i++) {
        org.w3c.dom.Element config = (org.w3c.dom.Element) configurations.item(i);
        org.w3c.dom.NodeList errorHandlers = config.getElementsByTagName("error-handler");
        if (errorHandlers.getLength() > 0) {
          hasGlobalErrorHandler = true;
          break;
        }
      }

      // Also check for error-handler at root level (global scope)
      if (!hasGlobalErrorHandler) {
        org.w3c.dom.Element root = parsedFile.xmlDocument.getDocumentElement();
        org.w3c.dom.NodeList rootLevelHandlers = root.getElementsByTagName("error-handler");
        for (int i = 0; i < rootLevelHandlers.getLength(); i++) {
          org.w3c.dom.Element handler = (org.w3c.dom.Element) rootLevelHandlers.item(i);
          // Check if error handler is at root level (not inside a flow)
          if (isGlobalErrorHandler(handler)) {
            hasGlobalErrorHandler = true;
            break;
          }
        }
      }

      // If there are flows but no global error handler, report issue
      if (!hasGlobalErrorHandler && !parsedFile.flows.isEmpty()) {
        reportIssue(context, inputFile,
            "Application does not have a global error handler configured. " +
            "Global error handlers provide a centralized way to handle unexpected errors " +
            "and ensure consistent error responses across all flows. " +
            "Add a global error handler in the configuration element.");
      }
    }
  }

  private boolean isGlobalErrorHandler(org.w3c.dom.Element handler) {
    org.w3c.dom.Node parent = handler.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      String nodeName = parent.getNodeName();
      // If inside a flow or sub-flow, it's not global
      if ("flow".equals(nodeName) || "sub-flow".equals(nodeName)) {
        return false;
      }
      // If inside configuration, it is global
      if ("configuration".equals(nodeName)) {
        return true;
      }
      parent = parent.getParentNode();
    }
    // If parent is root mule element, it's global
    return "mule".equals(parent != null ? parent.getNodeName() : "");
  }
}
