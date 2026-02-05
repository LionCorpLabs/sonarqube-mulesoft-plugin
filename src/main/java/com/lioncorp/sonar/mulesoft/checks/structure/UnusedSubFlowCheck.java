package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.HashSet;
import java.util.Set;

/**
 * Unused sub-flow.
 */
@Rule(key = "MS035")
public class UnusedSubFlowCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS035";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Collect all sub-flow names
    Set<String> subFlowNames = new HashSet<>();
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.isSubFlow && flow.name != null && !flow.name.isEmpty()) {
        subFlowNames.add(flow.name);
      }
    }

    // Collect all referenced flow names
    Set<String> referencedFlowNames = new HashSet<>();
    for (MuleSoftFileParser.FlowReference ref : parsedFile.flowReferences) {
      if (ref.flowName != null && !ref.flowName.isEmpty()) {
        referencedFlowNames.add(ref.flowName);
      }
    }

    // Find unused sub-flows
    for (String subFlowName : subFlowNames) {
      if (!referencedFlowNames.contains(subFlowName)) {
        reportIssue(context, inputFile,
            "Sub-flow '" + subFlowName + "' is never called. " +
            "Remove unused sub-flows to reduce code clutter and improve maintainability.");
      }
    }
  }
}
