package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing configuration.
 */
@Rule(key = "MS052")
public class MissingConfigurationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS052";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check if HTTP requests have proper configuration references
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request".equals(httpConfig.type)) {
        String configRef = httpConfig.element.getAttribute("config-ref");
        if (configRef == null || configRef.isEmpty()) {
          reportIssue(context, inputFile,
              "HTTP request missing required 'config-ref' attribute. " +
              "All HTTP requests should reference a configuration.");
        }
      }

      // Check if listener has proper configuration
      if ("listener".equals(httpConfig.type)) {
        String configRef = httpConfig.element.getAttribute("config-ref");
        if (configRef == null || configRef.isEmpty()) {
          reportIssue(context, inputFile,
              "HTTP listener missing required 'config-ref' attribute. " +
              "All HTTP listeners should reference a configuration.");
        }

        String path = httpConfig.element.getAttribute("path");
        if (path == null || path.isEmpty()) {
          reportIssue(context, inputFile,
              "HTTP listener missing required 'path' attribute.");
        }
      }
    }

    // Check if database operations have proper configuration references
    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      if (!"config".equals(dbConnector.type)) {
        String configRef = dbConnector.element.getAttribute("config-ref");
        if (configRef == null || configRef.isEmpty()) {
          reportIssue(context, inputFile,
              "Database operation '" + dbConnector.type + "' missing required 'config-ref' attribute. " +
              "All database operations should reference a configuration.");
        }
      }
    }

    // Check if flows with HTTP listeners have error handlers
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (!flow.isSubFlow && hasHttpListener(flow)) {
        if (!hasErrorHandler(flow)) {
          reportIssue(context, inputFile,
              "Flow '" + flow.name + "' has an HTTP listener but no error handler. " +
              "Flows with HTTP endpoints should have proper error handling configured.");
        }
      }
    }
  }

  private boolean hasHttpListener(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    org.w3c.dom.NodeList children = flow.element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        String nodeName = child.getNodeName();
        if (nodeName.equals("listener") || nodeName.endsWith(":listener")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasErrorHandler(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    org.w3c.dom.NodeList children = flow.element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        String nodeName = child.getNodeName();
        if ("error-handler".equals(nodeName)) {
          return true;
        }
      }
    }
    return false;
  }
}
