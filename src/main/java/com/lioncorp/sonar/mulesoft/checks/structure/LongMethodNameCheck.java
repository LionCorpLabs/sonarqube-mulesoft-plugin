package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Method name too long.
 */
@Rule(key = "MS049")
public class LongMethodNameCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS049";
  }
  private static final int MAX_NAME_LENGTH = 50;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.name != null && flow.name.length() > MAX_NAME_LENGTH) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " name '" + flow.name + "' is too long (" + flow.name.length() +
            " characters, exceeds " + MAX_NAME_LENGTH + " character limit). " +
            "Use shorter, more concise names for better readability.");
      }
    }
  }
}
