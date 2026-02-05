package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.*;

/**
 * Inappropriate intimacy - flows with too many dependencies.
 */
@Rule(key = "MS050")
public class InappropriateIntimacyCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS050";
  }
  private static final int MAX_FLOW_DEPENDENCIES = 5;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    Map<String, Set<String>> flowDependencies = buildDependencyGraph(parsedFile);

    for (Map.Entry<String, Set<String>> entry : flowDependencies.entrySet()) {
      String flowName = entry.getKey();
      Set<String> dependencies = entry.getValue();

      if (dependencies.size() > MAX_FLOW_DEPENDENCIES) {
        reportIssue(context, inputFile,
            "Flow '" + flowName + "' has " + dependencies.size() + " dependencies " +
            "(exceeds threshold of " + MAX_FLOW_DEPENDENCIES + "). " +
            "This indicates inappropriate intimacy. Consider reducing coupling by using an event-driven approach " +
            "or introducing an abstraction layer.");
      }
    }

    checkVariableAccess(context, inputFile, parsedFile);
  }

  private Map<String, Set<String>> buildDependencyGraph(MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    Map<String, Set<String>> dependencies = new HashMap<>();

    for (MuleSoftFileParser.FlowReference ref : parsedFile.flowReferences) {
      if (ref.sourceFlowName != null && ref.flowName != null) {
        dependencies.computeIfAbsent(ref.sourceFlowName, k -> new HashSet<>()).add(ref.flowName);
      }
    }

    return dependencies;
  }

  private void checkVariableAccess(SensorContext context, InputFile inputFile,
                                     MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.element == null) {
        continue;
      }

      Set<String> variablesAccessed = findVariableAccess(flow.element);
      if (variablesAccessed.size() > MAX_FLOW_DEPENDENCIES) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' accesses " + variablesAccessed.size() + " different variables " +
            "(exceeds threshold of " + MAX_FLOW_DEPENDENCIES + "). " +
            "This indicates tight coupling. Consider passing required data as parameters or using a data structure.");
      }
    }
  }

  private Set<String> findVariableAccess(org.w3c.dom.Element element) {
    Set<String> variables = new HashSet<>();
    String textContent = element.getTextContent();

    if (textContent != null) {
      java.util.regex.Pattern varPattern = java.util.regex.Pattern.compile("vars\\.(\\w+)");
      java.util.regex.Matcher matcher = varPattern.matcher(textContent);
      while (matcher.find()) {
        variables.add(matcher.group(1));
      }

      java.util.regex.Pattern flowVarPattern = java.util.regex.Pattern.compile("flowVars\\.(\\w+)");
      matcher = flowVarPattern.matcher(textContent);
      while (matcher.find()) {
        variables.add(matcher.group(1));
      }
    }

    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        variables.addAll(findVariableAccess((org.w3c.dom.Element) child));
      }
    }

    return variables;
  }
}
