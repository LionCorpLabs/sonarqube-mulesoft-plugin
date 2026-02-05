package com.lioncorp.sonar.mulesoft.checks;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;

/**
 * Check for empty flows in MuleSoft files.
 */
@Rule(key = "EmptyFlow")
public class EmptyFlowCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "EmptyFlow";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (isEmptyFlow(flow.element)) {
        reportIssue(context, inputFile, flow.name);
      }
    }
  }

  private boolean isEmptyFlow(Element flowElement) {
    // A flow is considered empty if it has no child elements (except maybe comments)
    return !flowElement.hasChildNodes() ||
           flowElement.getChildNodes().getLength() == 0;
  }
}
