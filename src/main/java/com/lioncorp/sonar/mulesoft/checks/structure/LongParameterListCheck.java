package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Long parameter list.
 */
@Rule(key = "MS044")
public class LongParameterListCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS044";
  }
  private static final int MAX_PARAMETERS = 7;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      int parameterCount = countParameters(flow);
      if (parameterCount > MAX_PARAMETERS) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' has too many parameters (" + parameterCount +
            ", exceeds threshold of " + MAX_PARAMETERS + "). " +
            "Consider grouping related parameters into an object or refactoring the flow.");
      }
    }
  }

  private int countParameters(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return 0;
    }

    int count = 0;

    // Count set-variable operations that appear to be parameters
    NodeList setVarNodes = flow.element.getElementsByTagName("set-variable");
    for (int i = 0; i < setVarNodes.getLength(); i++) {
      Element setVar = (Element) setVarNodes.item(i);
      // Check if this is likely a parameter (appears early in the flow)
      if (isLikelyParameter(setVar, flow)) {
        count++;
      }
    }

    // Count attributes that look like parameters
    String[] paramAttributes = {"queryParams", "uriParams", "headers"};
    for (String attr : paramAttributes) {
      if (flow.element.hasAttribute(attr)) {
        String value = flow.element.getAttribute(attr);
        if (value != null && !value.isEmpty()) {
          // Count comma-separated items as individual parameters
          count += value.split(",").length;
        }
      }
    }

    // Count flow-ref with target variables as parameters
    NodeList flowRefNodes = flow.element.getElementsByTagName("flow-ref");
    for (int i = 0; i < flowRefNodes.getLength(); i++) {
      Element flowRef = (Element) flowRefNodes.item(i);
      if (flowRef.hasAttribute("target")) {
        count++;
      }
    }

    return count;
  }

  private boolean isLikelyParameter(Element setVar, MuleSoftFileParser.MuleSoftFlow flow) {
    // Check if the set-variable is in the first few components of the flow
    NodeList siblings = flow.element.getChildNodes();
    int position = 0;
    for (int i = 0; i < siblings.getLength(); i++) {
      if (siblings.item(i) instanceof Element) {
        Element elem = (Element) siblings.item(i);
        String nodeName = elem.getNodeName();
        if (!"error-handler".equals(nodeName) && !"doc:description".equals(nodeName)) {
          position++;
          if (elem.isSameNode(setVar)) {
            return position <= 5; // Consider first 5 components as potential parameters
          }
        }
      }
    }
    return false;
  }
}
