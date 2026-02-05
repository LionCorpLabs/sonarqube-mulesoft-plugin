package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Vague flow name.
 */
@Rule(key = "MS060")
public class VagueFlowNameCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS060";
  }

  private static final String[] VAGUE_NAMES = {
      "process", "handle", "do", "main", "execute", "run", "test",
      "flow1", "flow2", "flow3", "temp", "sample", "example"
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.name != null && !flow.name.isEmpty() && isVagueName(flow.name)) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' has a vague name. Use descriptive names that explain the business purpose (e.g., 'retrieve-customer-data', 'validate-order-request').");
      }
    }
  }

  private boolean isVagueName(String name) {
    String lowerName = name.toLowerCase();
    for (String vagueWord : VAGUE_NAMES) {
      if (containsVagueWord(lowerName, vagueWord)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsVagueWord(String lowerName, String vagueWord) {
    return lowerName.equals(vagueWord) ||
           lowerName.startsWith(vagueWord + "-") ||
           lowerName.startsWith(vagueWord + "_") ||
           lowerName.endsWith("-" + vagueWord) ||
           lowerName.endsWith("_" + vagueWord);
  }
}
