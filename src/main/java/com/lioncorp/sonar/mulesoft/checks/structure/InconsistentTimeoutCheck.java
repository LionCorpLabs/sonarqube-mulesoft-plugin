package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

/**
 * Inconsistent timeout values.
 */
@Rule(key = "MS055")
public class InconsistentTimeoutCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS055";
  }
  private static final int THRESHOLD_DIFFERENCE = 5000; // 5 seconds

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    Map<String, Integer> timeoutsByType = new HashMap<>();
    Map<String, Integer> countsByType = new HashMap<>();

    // Collect timeout values from HTTP requests
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request".equals(httpConfig.type)) {
        String timeoutStr = httpConfig.element.getAttribute("responseTimeout");
        if (timeoutStr != null && !timeoutStr.isEmpty()) {
          try {
            int timeout = parseTimeout(timeoutStr);
            String type = "http:request";
            timeoutsByType.put(type, timeoutsByType.getOrDefault(type, 0) + timeout);
            countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
          } catch (NumberFormatException ignored) {
            // Skip non-numeric timeouts
          }
        }
      }
    }

    // Check database connectors for timeouts
    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      String timeoutStr = dbConnector.element.getAttribute("queryTimeout");
      if (timeoutStr != null && !timeoutStr.isEmpty()) {
        try {
          int timeout = parseTimeout(timeoutStr);
          String type = "db:" + dbConnector.type;
          timeoutsByType.put(type, timeoutsByType.getOrDefault(type, 0) + timeout);
          countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
        } catch (NumberFormatException ignored) {
          // Skip non-numeric timeouts
        }
      }
    }

    // Scan all elements for timeout attributes
    if (parsedFile.xmlDocument != null) {
      scanElementForTimeouts(parsedFile.xmlDocument.getDocumentElement(), timeoutsByType, countsByType);
    }

    // Check for inconsistencies
    for (Map.Entry<String, Integer> entry : timeoutsByType.entrySet()) {
      String type = entry.getKey();
      int totalTimeout = entry.getValue();
      int count = countsByType.get(type);

      if (count > 1) {
        int avgTimeout = totalTimeout / count;
        // Check if there's significant variation
        if (hasSignificantVariation(parsedFile, type, avgTimeout)) {
          reportIssue(context, inputFile,
              "Inconsistent timeout values detected for '" + type + "' components. " +
              "Average timeout is " + avgTimeout + "ms. Consider standardizing timeout values across similar components.");
        }
      }
    }
  }

  private void scanElementForTimeouts(Element element, Map<String, Integer> timeoutsByType, Map<String, Integer> countsByType) {
    // Check for timeout attributes
    String[] timeoutAttrs = {"timeout", "responseTimeout", "queryTimeout", "connectionTimeout", "socketTimeout"};
    for (String attr : timeoutAttrs) {
      String timeoutStr = element.getAttribute(attr);
      if (timeoutStr != null && !timeoutStr.isEmpty()) {
        try {
          int timeout = parseTimeout(timeoutStr);
          String type = element.getTagName() + ":" + attr;
          timeoutsByType.put(type, timeoutsByType.getOrDefault(type, 0) + timeout);
          countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
        } catch (NumberFormatException ignored) {
          // Skip non-numeric timeouts
        }
      }
    }

    // Recursively check child elements
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element) {
        scanElementForTimeouts((Element) children.item(i), timeoutsByType, countsByType);
      }
    }
  }

  private boolean hasSignificantVariation(MuleSoftFileParser.ParsedMuleSoftFile parsedFile, String type, int avgTimeout) {
    // Check HTTP requests
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if (("http:request".equals(type)) && "request".equals(httpConfig.type)) {
        String timeoutStr = httpConfig.element.getAttribute("responseTimeout");
        if (timeoutStr != null && !timeoutStr.isEmpty()) {
          try {
            int timeout = parseTimeout(timeoutStr);
            if (Math.abs(timeout - avgTimeout) > THRESHOLD_DIFFERENCE) {
              return true;
            }
          } catch (NumberFormatException ignored) {
            // Skip
          }
        }
      }
    }

    // Check database connectors
    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      if (type.startsWith("db:")) {
        String timeoutStr = dbConnector.element.getAttribute("queryTimeout");
        if (timeoutStr != null && !timeoutStr.isEmpty()) {
          try {
            int timeout = parseTimeout(timeoutStr);
            if (Math.abs(timeout - avgTimeout) > THRESHOLD_DIFFERENCE) {
              return true;
            }
          } catch (NumberFormatException ignored) {
            // Skip
          }
        }
      }
    }

    return false;
  }

  private int parseTimeout(String timeoutStr) throws NumberFormatException {
    // Remove any units and parse
    String cleaned = timeoutStr.replaceAll("[^0-9]", "");
    return Integer.parseInt(cleaned);
  }
}
