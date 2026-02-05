package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Variable name too long.
 */
@Rule(key = "MS070")
public class TooLongVariableNameCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS070";
  }

  private static final int MAX_NAME_LENGTH = 50;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flow names
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.name != null && flow.name.length() > MAX_NAME_LENGTH) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " name '" + flow.name + "' is too long (" + flow.name.length() + " characters). Keep names under " + MAX_NAME_LENGTH + " characters for readability.");
      }
    }

    // Check variable names in content
    checkVariableNamesInContent(context, inputFile, parsedFile.rawContent);
  }

  private void checkVariableNamesInContent(SensorContext context, InputFile inputFile, String content) {
    // Simple regex to find variableName attributes
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("variableName=[\"']([^\"']+)[\"']");
    java.util.regex.Matcher matcher = pattern.matcher(content);

    while (matcher.find()) {
      String varName = matcher.group(1);
      if (varName.length() > MAX_NAME_LENGTH) {
        reportIssue(context, inputFile,
            "Variable name '" + varName + "' is too long (" + varName.length() + " characters). Keep names under " + MAX_NAME_LENGTH + " characters.");
        break;
      }
    }
  }
}
