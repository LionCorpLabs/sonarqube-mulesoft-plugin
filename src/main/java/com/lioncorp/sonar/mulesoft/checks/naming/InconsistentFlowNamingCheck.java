package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Inconsistent flow naming.
 */
@Rule(key = "MS069")
public class InconsistentFlowNamingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS069";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Detect naming patterns across flows
    String detectedPattern = detectNamingPattern(parsedFile.flows);

    if (detectedPattern != null) {
      for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
        if (flow.name != null && !flow.name.isEmpty() && !matchesPattern(flow.name, detectedPattern)) {
          String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
          reportIssue(context, inputFile,
              flowType + " '" + flow.name + "' does not follow the project's naming pattern '" + detectedPattern + "'. Maintain consistent naming conventions.");
        }
      }
    }
  }

  private String detectNamingPattern(java.util.List<MuleSoftFileParser.MuleSoftFlow> flows) {
    // Check for common patterns like verb-noun, noun-verb, etc.
    int verbNounCount = 0;
    int nounVerbCount = 0;

    for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
      if (flow.name != null && !flow.name.isEmpty()) {
        if (startsWithVerb(flow.name)) {
          verbNounCount++;
        } else if (endsWithVerb(flow.name)) {
          nounVerbCount++;
        }
      }
    }

    if (verbNounCount > nounVerbCount && verbNounCount > flows.size() / 2) {
      return "verb-noun (e.g., get-customer, create-order)";
    } else if (nounVerbCount > verbNounCount && nounVerbCount > flows.size() / 2) {
      return "noun-verb (e.g., customer-get, order-create)";
    }
    return null;
  }

  private boolean matchesPattern(String name, String pattern) {
    if (pattern.startsWith("verb-noun")) {
      return startsWithVerb(name);
    } else if (pattern.startsWith("noun-verb")) {
      return endsWithVerb(name);
    }
    return true;
  }

  private boolean startsWithVerb(String name) {
    String[] commonVerbs = {"get", "post", "put", "delete", "create", "update", "fetch",
                            "retrieve", "validate", "process", "transform", "send", "receive"};
    String lowerName = name.toLowerCase();
    for (String verb : commonVerbs) {
      if (lowerName.startsWith(verb + "-") || lowerName.startsWith(verb + "_")) {
        return true;
      }
    }
    return false;
  }

  private boolean endsWithVerb(String name) {
    String[] commonVerbs = {"get", "post", "put", "delete", "create", "update", "fetch",
                            "retrieve", "validate", "process", "transform", "send", "receive"};
    String lowerName = name.toLowerCase();
    for (String verb : commonVerbs) {
      if (lowerName.endsWith("-" + verb) || lowerName.endsWith("_" + verb)) {
        return true;
      }
    }
    return false;
  }
}
