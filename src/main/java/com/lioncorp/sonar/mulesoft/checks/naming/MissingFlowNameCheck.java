package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing flow name.
 */
@Rule(key = "MS059")
public class MissingFlowNameCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS059";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.name == null || flow.name.trim().isEmpty()) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " is missing a name attribute. All flows must have descriptive names for maintainability.",
            flow.lineNumber);
      }
    }
  }
}
