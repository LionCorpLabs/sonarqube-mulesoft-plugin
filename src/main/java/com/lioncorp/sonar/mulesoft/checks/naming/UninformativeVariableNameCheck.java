package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Uninformative variable name.
 */
@Rule(key = "MS064")
public class UninformativeVariableNameCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS064";
  }

  private static final String[] UNINFORMATIVE_NAMES = {
      "temp", "tmp", "data", "var", "variable", "value", "val",
      "result", "response", "request", "item", "element", "obj",
      "thing", "stuff", "foo", "bar", "baz", "x", "y", "z"
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    String content = parsedFile.rawContent;

    // Check for variableName attributes
    for (String uninformative : UNINFORMATIVE_NAMES) {
      String pattern1 = "variableName=\"" + uninformative + "\"";
      String pattern2 = "variableName='" + uninformative + "'";

      if (content.contains(pattern1) || content.contains(pattern2)) {
        reportIssue(context, inputFile,
            "Variable name '" + uninformative + "' is uninformative. Use descriptive names that indicate the variable's purpose (e.g., 'customerId', 'orderTotal').");
        break;
      }
    }
  }
}
