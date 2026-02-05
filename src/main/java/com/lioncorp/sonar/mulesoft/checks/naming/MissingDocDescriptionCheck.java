package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing doc description.
 */
@Rule(key = "MS065")
public class MissingDocDescriptionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS065";
  }

  private static final int MIN_DESCRIPTION_LENGTH = 10;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.element != null) {
        checkFlowDocumentation(context, inputFile, flow);
      }
    }
  }

  private void checkFlowDocumentation(SensorContext context, InputFile inputFile, MuleSoftFileParser.MuleSoftFlow flow) {
    org.w3c.dom.NodeList docNodes = flow.element.getElementsByTagName("doc:description");
    String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";

    if (docNodes.getLength() == 0) {
      reportIssue(context, inputFile,
          flowType + " '" + flow.name + "' is missing a doc:description element. Add documentation to explain the flow's purpose and behavior.");
    } else {
      checkDescriptionQuality(context, inputFile, docNodes, flow.name, flowType);
    }
  }

  private void checkDescriptionQuality(SensorContext context, InputFile inputFile, org.w3c.dom.NodeList docNodes,
                                       String flowName, String flowType) {
    String description = docNodes.item(0).getTextContent();
    if (description == null || description.trim().isEmpty() || description.trim().length() < MIN_DESCRIPTION_LENGTH) {
      reportIssue(context, inputFile,
          flowType + " '" + flowName + "' has an empty or too short doc:description. Provide meaningful documentation.");
    }
  }
}
