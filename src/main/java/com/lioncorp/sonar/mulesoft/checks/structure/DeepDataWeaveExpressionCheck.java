package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Deep DataWeave nesting.
 */
@Rule(key = "MS043")
public class DeepDataWeaveExpressionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS043";
  }

  private static final int MAX_NESTING_DEPTH = 4;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check DataWeave expressions in transform components
    if (parsedFile.xmlDocument != null) {
      checkDataWeaveExpressions(context, inputFile, parsedFile.xmlDocument);
    }
  }

  private void checkDataWeaveExpressions(SensorContext context, InputFile inputFile,
                                          org.w3c.dom.Document document) {
    // Find all ee:transform elements
    org.w3c.dom.NodeList transformNodes = document.getElementsByTagNameNS("*", "transform");
    for (int i = 0; i < transformNodes.getLength(); i++) {
      org.w3c.dom.Element transformElement = (org.w3c.dom.Element) transformNodes.item(i);
      checkTransformElement(context, inputFile, transformElement);
    }

    // Find all set-payload and set-variable with DataWeave
    String[] dataWeaveElements = {"set-payload", "set-variable", "set-property"};
    for (String elementName : dataWeaveElements) {
      org.w3c.dom.NodeList nodes = document.getElementsByTagName(elementName);
      for (int i = 0; i < nodes.getLength(); i++) {
        org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);
        checkDataWeaveInElement(context, inputFile, element);
      }
    }
  }

  private void checkTransformElement(SensorContext context, InputFile inputFile,
                                      org.w3c.dom.Element transformElement) {
    String textContent = transformElement.getTextContent();
    if (textContent != null && !textContent.isEmpty()) {
      int maxDepth = calculateDataWeaveNestingDepth(textContent);
      if (maxDepth > MAX_NESTING_DEPTH) {
        reportIssue(context, inputFile,
            "DataWeave expression has nesting depth of " + maxDepth +
            " (exceeds threshold of " + MAX_NESTING_DEPTH + "). " +
            "Consider breaking complex transformations into smaller steps or using separate variables.");
      }
    }
  }

  private void checkDataWeaveInElement(SensorContext context, InputFile inputFile,
                                        org.w3c.dom.Element element) {
    String value = element.getAttribute("value");
    if (value != null && !value.isEmpty() && looksLikeDataWeave(value)) {
      int maxDepth = calculateDataWeaveNestingDepth(value);
      if (maxDepth > MAX_NESTING_DEPTH) {
        reportIssue(context, inputFile,
            "DataWeave expression in '" + element.getTagName() + "' has nesting depth of " + maxDepth +
            " (exceeds threshold of " + MAX_NESTING_DEPTH + "). " +
            "Consider simplifying the expression.");
      }
    }
  }

  private boolean looksLikeDataWeave(String value) {
    return value.contains("%dw") || value.contains("payload") || value.contains("attributes");
  }

  private int calculateDataWeaveNestingDepth(String expression) {
    int maxDepth = 0;
    int currentDepth = 0;

    for (char c : expression.toCharArray()) {
      if (c == '(' || c == '[' || c == '{') {
        currentDepth++;
        maxDepth = Math.max(maxDepth, currentDepth);
      } else if (c == ')' || c == ']' || c == '}') {
        currentDepth--;
      }
    }

    return maxDepth;
  }
}
