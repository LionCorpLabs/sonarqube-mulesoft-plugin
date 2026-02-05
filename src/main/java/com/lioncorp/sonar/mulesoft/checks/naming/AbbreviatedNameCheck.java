package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Excessive abbreviations.
 */
@Rule(key = "MS062")
public class AbbreviatedNameCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS062";
  }

  private static final String[] EXCESSIVE_ABBREVIATIONS = {
      "tmp", "temp", "mgr", "mngr", "proc", "btn", "lbl", "txt",
      "num", "str", "obj", "arr", "ptr", "ref", "val", "var",
      "msg", "req", "res", "resp", "prm", "param", "cfg", "cnfg"
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flow names
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.name != null && hasExcessiveAbbreviations(flow.name)) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' contains excessive abbreviations. Use full words for better readability (e.g., 'temp' -> 'temporary', 'msg' -> 'message').");
      }
    }

    // Check variable names in raw content (set-variable, set-payload)
    checkVariableNames(context, inputFile, parsedFile.rawContent);
  }

  private boolean hasExcessiveAbbreviations(String name) {
    String lowerName = name.toLowerCase();
    for (String abbr : EXCESSIVE_ABBREVIATIONS) {
      if (lowerName.contains(abbr)) {
        return true;
      }
    }
    return false;
  }

  private void checkVariableNames(SensorContext context, InputFile inputFile, String content) {
    // Check for variableName attributes in set-variable
    for (String abbr : EXCESSIVE_ABBREVIATIONS) {
      if (content.contains("variableName=\"" + abbr) || content.contains("variableName='" + abbr)) {
        reportIssue(context, inputFile,
            "Variable name contains abbreviation '" + abbr + "'. Use descriptive names instead.");
        break;
      }
    }
  }
}
