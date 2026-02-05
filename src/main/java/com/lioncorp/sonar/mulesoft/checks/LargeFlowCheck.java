package com.lioncorp.sonar.mulesoft.checks;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Check for flows that are too large and should be broken down into sub-flows.
 */
@Rule(key = "LargeFlow")
public class LargeFlowCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "LargeFlow";
  }
  private static final int MAX_COMPONENTS = 15;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      int componentCount = countComponents(flow.element);
      if (componentCount > MAX_COMPONENTS) {
        reportIssue(context, inputFile, flow.name, componentCount);
      }
    }
  }

  private int countComponents(Element flowElement) {
    int count = 0;
    NodeList children = flowElement.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        count++;
      }
    }
    return count;
  }
}
