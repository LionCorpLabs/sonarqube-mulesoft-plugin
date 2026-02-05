package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Empty flow detected.
 */
@Rule(key = "MS031")
public class EmptyFlowCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS031";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (isFlowEmpty(flow)) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' is empty and contains no components.", flow.lineNumber);
      }
    }
  }

  private boolean isFlowEmpty(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return true;
    }

    // Check if flow has any child elements (excluding error-handler)
    org.w3c.dom.NodeList children = flow.element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        String nodeName = child.getNodeName();
        // Exclude error-handler and doc elements from component count
        if (!"error-handler".equals(nodeName) && !"doc:description".equals(nodeName)) {
          return false;
        }
      }
    }
    return true;
  }
}
