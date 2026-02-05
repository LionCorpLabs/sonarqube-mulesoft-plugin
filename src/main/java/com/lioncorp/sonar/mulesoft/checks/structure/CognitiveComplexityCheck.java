package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * High cognitive complexity.
 */
@Rule(key = "MS041")
public class CognitiveComplexityCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS041";
  }

  private static final int COMPLEXITY_THRESHOLD = 15;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      int complexity = calculateCognitiveComplexity(flow);
      if (complexity > COMPLEXITY_THRESHOLD) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' has cognitive complexity of " + complexity +
            " (exceeds threshold of " + COMPLEXITY_THRESHOLD + "). " +
            "Consider breaking it into smaller, more understandable flows.");
      }
    }
  }

  private int calculateCognitiveComplexity(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return 0;
    }

    int complexity = 0;
    complexity += countComplexityRecursive(flow.element, 0);
    return complexity;
  }

  private int countComplexityRecursive(org.w3c.dom.Element element, int nestingLevel) {
    int complexity = 0;

    // Elements that increase cognitive complexity
    String tagName = element.getTagName();

    // Conditional structures add complexity based on nesting
    if (isConditionalStructure(tagName)) {
      complexity += (1 + nestingLevel);
    }

    // Loops add complexity
    if (isLoopStructure(tagName)) {
      complexity += (1 + nestingLevel);
    }

    // Exception handling adds complexity
    if (isErrorHandlingStructure(tagName)) {
      complexity += 1;
    }

    // Recursively process child elements
    org.w3c.dom.NodeList children = element.getChildNodes();
    int newNestingLevel = isNestingIncreaser(tagName) ? nestingLevel + 1 : nestingLevel;

    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        complexity += countComplexityRecursive((org.w3c.dom.Element) child, newNestingLevel);
      }
    }

    return complexity;
  }

  private boolean isConditionalStructure(String tagName) {
    return tagName.equals("choice") ||
           tagName.equals("when") ||
           tagName.contains("if");
  }

  private boolean isLoopStructure(String tagName) {
    return tagName.equals("foreach") ||
           tagName.equals("for-each") ||
           tagName.equals("while") ||
           tagName.equals("until-successful");
  }

  private boolean isErrorHandlingStructure(String tagName) {
    return tagName.equals("try") ||
           tagName.equals("error-handler") ||
           tagName.equals("on-error-continue") ||
           tagName.equals("on-error-propagate");
  }

  private boolean isNestingIncreaser(String tagName) {
    return isConditionalStructure(tagName) ||
           isLoopStructure(tagName) ||
           tagName.equals("try") ||
           tagName.equals("async");
  }
}
