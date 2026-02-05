package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Unoptimized DataWeave.
 */
@Rule(key = "MS084")
public class UnoptimizedDataWeaveScriptCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS084";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Find all DataWeave transformations
    org.w3c.dom.NodeList transformNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "transform");

    for (int i = 0; i < transformNodes.getLength(); i++) {
      org.w3c.dom.Element transformElement = (org.w3c.dom.Element) transformNodes.item(i);
      String dwScript = extractDataWeaveScript(transformElement);

      if (dwScript != null && !dwScript.trim().isEmpty()) {
        checkDataWeaveOptimizations(context, inputFile, dwScript);
      }
    }

    // Also check set-payload and set-variable with DataWeave
    checkSetPayloadTransformations(context, inputFile, parsedFile);
  }

  private String extractDataWeaveScript(org.w3c.dom.Element transformElement) {
    StringBuilder script = new StringBuilder();
    org.w3c.dom.NodeList children = transformElement.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE || child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
        String content = child.getTextContent();
        if (content != null && !content.trim().isEmpty()) {
          script.append(content);
        }
      } else if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;
        if ("message".equals(childElement.getLocalName()) ||
            "set-payload".equals(childElement.getLocalName()) ||
            "set-variable".equals(childElement.getLocalName())) {
          script.append(extractDataWeaveScript(childElement));
        }
      }
    }

    return script.toString();
  }

  private void checkSetPayloadTransformations(SensorContext context, InputFile inputFile,
                                             MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    org.w3c.dom.NodeList setPayloadNodes = parsedFile.xmlDocument.getElementsByTagName("set-payload");

    for (int i = 0; i < setPayloadNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) setPayloadNodes.item(i);
      String value = element.getAttribute("value");

      if (value != null && (value.contains("#[") || value.contains("%dw"))) {
        checkDataWeaveOptimizations(context, inputFile, value);
      }
    }

    org.w3c.dom.NodeList setVariableNodes = parsedFile.xmlDocument.getElementsByTagName("set-variable");

    for (int i = 0; i < setVariableNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) setVariableNodes.item(i);
      String value = element.getAttribute("value");

      if (value != null && (value.contains("#[") || value.contains("%dw"))) {
        checkDataWeaveOptimizations(context, inputFile, value);
      }
    }
  }

  private void checkDataWeaveOptimizations(SensorContext context, InputFile inputFile, String dwScript) {
    // Check for inefficient array indexing in loops
    if (dwScript.matches("(?s).*map\\s*\\(.*\\[\\d+\\].*\\).*")) {
      reportIssue(context, inputFile,
          "Array indexing inside map operation detected. This can be inefficient. " +
          "Consider using item notation or restructuring the data access pattern.");
    }

    // Check for repeated computations that should be in variables
    if (hasRepeatedExpressions(dwScript)) {
      reportIssue(context, inputFile,
          "Repeated expressions detected in DataWeave script. Consider storing computed " +
          "values in variables to avoid redundant calculations.");
    }

    // Check for using pluck when keys/values would suffice
    if (dwScript.contains("pluck") && (dwScript.contains(".$") || dwScript.contains(".$$"))) {
      reportIssue(context, inputFile,
          "Using pluck to extract only keys or values. Consider using keysOf() or " +
          "valuesOf() functions instead for better performance.");
    }

    // Check for unnecessary type coercion
    if (countOccurrences(dwScript, " as ") > 3) {
      reportIssue(context, inputFile,
          "Multiple type coercions detected. Excessive type conversions can impact performance. " +
          "Consider restructuring data flow or using consistent types.");
    }

    // Check for string concatenation in loops
    if (dwScript.matches("(?s).*map\\s*\\(.*\\+\\+.*\\).*") ||
        dwScript.matches("(?s).*map\\s*\\(.*\".*\"\\s*\\+\\s*.*\\).*")) {
      reportIssue(context, inputFile,
          "String concatenation inside map operation. For large datasets, this can be " +
          "inefficient. Consider using string interpolation or join functions.");
    }

    // Check for filter followed by map (should use reduce or combined operation)
    if (dwScript.matches("(?s).*filter\\s*\\([^)]+\\)\\s*map\\s*\\(.*")) {
      reportIssue(context, inputFile,
          "Filter followed by map detected. For large datasets, consider combining operations " +
          "or using reduce for better performance.");
    }

    // Check for isEmpty() on large collections (should use sizeOf == 0 or other checks)
    if (dwScript.contains("isEmpty(payload)") || dwScript.matches("(?s).*isEmpty\\([^)]*payload[^)]*\\).*")) {
      reportIssue(context, inputFile,
          "isEmpty() on payload detected. For large payloads, this might materialize the " +
          "entire collection. Consider using alternative checks if possible.");
    }

    // Check for orderBy without limit
    if (dwScript.contains("orderBy") && !dwScript.contains("[0 to ") && !dwScript.contains("take(")) {
      reportIssue(context, inputFile,
          "orderBy without limiting results detected. Sorting large datasets can be expensive. " +
          "If you only need top N items, consider using range selector [0 to N] or take().");
    }

    // Check for using ++ in reduce (should use + or append)
    if (dwScript.matches("(?s).*reduce\\s*\\([^)]*\\+\\+.*\\).*")) {
      reportIssue(context, inputFile,
          "Using ++ operator in reduce operation. For array concatenation in reduce, " +
          "this can cause performance issues. Consider using + operator or restructuring.");
    }

    // Check for default operator overuse
    if (countOccurrences(dwScript, " default ") > 5) {
      reportIssue(context, inputFile,
          "Excessive use of default operator. This might indicate data quality issues. " +
          "Consider validating input data structure or using optional chain operators.");
    }
  }

  private boolean hasRepeatedExpressions(String dwScript) {
    // Simple heuristic: look for common patterns that appear multiple times
    String[] patterns = {
        "payload\\.",
        "vars\\.",
        "attributes\\."
    };

    for (String pattern : patterns) {
      int count = countOccurrences(dwScript, pattern);
      if (count > 5) {
        // Check if same expression appears multiple times
        if (dwScript.matches("(?s).*" + pattern + "\\w+.*" + pattern + "\\w+.*")) {
          return true;
        }
      }
    }

    return false;
  }

  private int countOccurrences(String text, String pattern) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(pattern, index)) != -1) {
      count++;
      index += pattern.length();
    }
    return count;
  }
}
