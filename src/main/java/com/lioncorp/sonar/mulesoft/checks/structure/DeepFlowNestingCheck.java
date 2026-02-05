package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Excessive flow nesting.
 */
@Rule(key = "MS034")
public class DeepFlowNestingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS034";
  }

  private static final int MAX_NESTING_LEVEL = 3;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      int maxNesting = calculateMaxNestingLevel(flow);
      if (maxNesting > MAX_NESTING_LEVEL) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' has maximum nesting level of " + maxNesting +
            " (exceeds threshold of " + MAX_NESTING_LEVEL + "). " +
            "Deep nesting makes code hard to understand. Consider flattening the structure or extracting nested logic.");
      }
    }
  }

  private int calculateMaxNestingLevel(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return 0;
    }
    return calculateNestingRecursive(flow.element, 0);
  }

  private int calculateNestingRecursive(org.w3c.dom.Element element, int currentLevel) {
    int maxLevel = currentLevel;

    String tagName = element.getTagName();
    int newLevel = isNestingElement(tagName) ? currentLevel + 1 : currentLevel;

    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        int childMax = calculateNestingRecursive((org.w3c.dom.Element) child, newLevel);
        maxLevel = Math.max(maxLevel, childMax);
      }
    }

    return maxLevel;
  }

  private boolean isNestingElement(String tagName) {
    return tagName.equals("choice") ||
           tagName.equals("when") ||
           tagName.equals("otherwise") ||
           tagName.equals("foreach") ||
           tagName.equals("for-each") ||
           tagName.equals("while") ||
           tagName.equals("try") ||
           tagName.equals("scope") ||
           tagName.equals("async") ||
           tagName.equals("until-successful") ||
           tagName.equals("scatter-gather");
  }
}
