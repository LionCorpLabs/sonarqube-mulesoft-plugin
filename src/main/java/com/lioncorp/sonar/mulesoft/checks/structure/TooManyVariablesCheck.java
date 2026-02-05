package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashSet;
import java.util.Set;

/**
 * Too many variables.
 */
@Rule(key = "MS048")
public class TooManyVariablesCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS048";
  }
  private static final int MAX_VARIABLES = 10;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      int variableCount = countVariables(flow);
      if (variableCount > MAX_VARIABLES) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' has too many variables (" + variableCount +
            ", exceeds threshold of " + MAX_VARIABLES + "). " +
            "High variable count indicates complexity. Consider refactoring into smaller flows.");
      }
    }
  }

  private int countVariables(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return 0;
    }

    Set<String> uniqueVariables = new HashSet<>();

    // Count set-variable operations
    NodeList setVarNodes = flow.element.getElementsByTagName("set-variable");
    for (int i = 0; i < setVarNodes.getLength(); i++) {
      Element setVar = (Element) setVarNodes.item(i);
      String varName = setVar.getAttribute("variableName");
      if (varName != null && !varName.isEmpty()) {
        uniqueVariables.add(varName);
      }
    }

    // Count remove-variable operations (indicates variable usage)
    NodeList removeVarNodes = flow.element.getElementsByTagName("remove-variable");
    for (int i = 0; i < removeVarNodes.getLength(); i++) {
      Element removeVar = (Element) removeVarNodes.item(i);
      String varName = removeVar.getAttribute("variableName");
      if (varName != null && !varName.isEmpty()) {
        uniqueVariables.add(varName);
      }
    }

    // Count target variables in flow-ref calls
    NodeList flowRefNodes = flow.element.getElementsByTagName("flow-ref");
    for (int i = 0; i < flowRefNodes.getLength(); i++) {
      Element flowRef = (Element) flowRefNodes.item(i);
      String target = flowRef.getAttribute("target");
      if (target != null && !target.isEmpty()) {
        uniqueVariables.add(target);
      }
    }

    // Count target variables in other operations (transform, set-payload with target)
    String[] targetElements = {"ee:transform", "set-payload", "http:request", "db:select"};
    for (String tagName : targetElements) {
      NodeList nodes = flow.element.getElementsByTagName(tagName);
      for (int i = 0; i < nodes.getLength(); i++) {
        Element elem = (Element) nodes.item(i);
        String target = elem.getAttribute("target");
        if (target != null && !target.isEmpty()) {
          uniqueVariables.add(target);
        }
      }
    }

    return uniqueVariables.size();
  }
}
