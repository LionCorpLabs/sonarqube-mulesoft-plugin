package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Flow too large.
 */
@Rule(key = "MS032")
public class LargeFlowCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS032";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      int componentCount = countComponents(flow);
      if (componentCount > 15) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' has " + componentCount +
            " components (exceeds threshold of 15). Consider breaking it into smaller flows.",
            flow.lineNumber);
      }
    }
  }

  private int countComponents(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return 0;
    }

    int count = 0;
    org.w3c.dom.NodeList children = flow.element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        String nodeName = child.getNodeName();
        // Exclude error-handler and doc elements from component count
        if (!"error-handler".equals(nodeName) && !"doc:description".equals(nodeName)) {
          count++;
        }
      }
    }
    return count;
  }
}
