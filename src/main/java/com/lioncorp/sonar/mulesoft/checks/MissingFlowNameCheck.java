package com.lioncorp.sonar.mulesoft.checks;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Check for flows without names.
 */
@Rule(key = "MissingFlowName")
public class MissingFlowNameCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MissingFlowName";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.name == null || flow.name.trim().isEmpty()) {
        reportIssue(context, inputFile, "Flow without a name detected. All flows should have descriptive names.");
      }
    }
  }
}
