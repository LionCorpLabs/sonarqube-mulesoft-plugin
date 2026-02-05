package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Complex boolean expression.
 */
@Rule(key = "MS046")
public class ComplexBooleanExpressionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS046";
  }

  private static final int MAX_BOOLEAN_OPERATORS = 3;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check choice router conditions
    for (MuleSoftFileParser.ChoiceRouter router : parsedFile.choiceRouters) {
      checkChoiceConditions(context, inputFile, router);
    }

    // Check DataWeave expressions in all flows
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      checkFlowExpressions(context, inputFile, flow);
    }
  }

  private void checkChoiceConditions(SensorContext context, InputFile inputFile,
                                      MuleSoftFileParser.ChoiceRouter router) {
    if (router.element == null) {
      return;
    }

    org.w3c.dom.NodeList whenNodes = router.element.getElementsByTagName("when");
    for (int i = 0; i < whenNodes.getLength(); i++) {
      org.w3c.dom.Element whenElement = (org.w3c.dom.Element) whenNodes.item(i);
      String expression = whenElement.getAttribute("expression");

      if (expression != null && !expression.isEmpty()) {
        int operatorCount = countBooleanOperators(expression);
        if (operatorCount > MAX_BOOLEAN_OPERATORS) {
          reportIssue(context, inputFile,
              "Boolean expression in choice router has " + operatorCount + " operators " +
              "(exceeds threshold of " + MAX_BOOLEAN_OPERATORS + "). " +
              "Consider extracting sub-expressions into variables or using separate when clauses.");
        }
      }
    }
  }

  private void checkFlowExpressions(SensorContext context, InputFile inputFile,
                                     MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return;
    }

    // Check all elements with expressions
    checkExpressionsInElement(context, inputFile, flow.element);
  }

  private void checkExpressionsInElement(SensorContext context, InputFile inputFile,
                                          org.w3c.dom.Element element) {
    // Check attributes that might contain expressions
    org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      org.w3c.dom.Node attr = attributes.item(i);
      String attrName = attr.getNodeName();
      String attrValue = attr.getNodeValue();

      if (attrName.contains("expression") || attrName.equals("if") || attrName.equals("unless")) {
        int operatorCount = countBooleanOperators(attrValue);
        if (operatorCount > MAX_BOOLEAN_OPERATORS) {
          reportIssue(context, inputFile,
              "Boolean expression in '" + element.getTagName() + "' has " + operatorCount + " operators " +
              "(exceeds threshold of " + MAX_BOOLEAN_OPERATORS + "). " +
              "Consider simplifying the expression or using intermediate variables.");
        }
      }
    }

    // Recursively check child elements
    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        checkExpressionsInElement(context, inputFile, (org.w3c.dom.Element) child);
      }
    }
  }

  private int countBooleanOperators(String expression) {
    if (expression == null || expression.isEmpty()) {
      return 0;
    }

    int count = 0;
    // Count logical operators: and, or, &&, ||
    count += countOccurrences(expression, " and ");
    count += countOccurrences(expression, " or ");
    count += countOccurrences(expression, "&&");
    count += countOccurrences(expression, "||");
    count += countOccurrences(expression, " AND ");
    count += countOccurrences(expression, " OR ");

    return count;
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
