package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.HashMap;
import java.util.Map;

/**
 * Too many flow-refs.
 */
@Rule(key = "MS038")
public class TooManyFlowRefsCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS038";
  }
  private static final int MAX_FLOW_REFS = 7;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Count flow-refs per flow
    Map<String, Integer> flowRefCounts = new HashMap<>();

    for (MuleSoftFileParser.FlowReference ref : parsedFile.flowReferences) {
      if (ref.sourceFlowName != null && !ref.sourceFlowName.isEmpty()) {
        flowRefCounts.put(ref.sourceFlowName, flowRefCounts.getOrDefault(ref.sourceFlowName, 0) + 1);
      }
    }

    // Report flows with too many flow-refs
    for (Map.Entry<String, Integer> entry : flowRefCounts.entrySet()) {
      String flowName = entry.getKey();
      int count = entry.getValue();

      if (count > MAX_FLOW_REFS) {
        reportIssue(context, inputFile,
            "Flow '" + flowName + "' has too many flow-ref calls (" + count +
            ", exceeds threshold of " + MAX_FLOW_REFS + "). " +
            "This indicates high coupling. Consider consolidating or refactoring the flow structure.");
      }
    }
  }
}
