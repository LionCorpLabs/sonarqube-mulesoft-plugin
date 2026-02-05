package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.*;

/**
 * Inconsistent flow structure patterns.
 */
@Rule(key = "MS039")
public class InconsistentFlowStructureCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS039";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.flows.size() < 2) {
      return;
    }

    Map<String, List<MuleSoftFileParser.MuleSoftFlow>> flowsByPattern = categorizeFlowsByPattern(parsedFile.flows);

    checkErrorHandlerConsistency(context, inputFile, parsedFile.flows);
    checkLoggerUsageConsistency(context, inputFile, parsedFile.flows, parsedFile.loggers);
    checkStructuralPatterns(context, inputFile, flowsByPattern);
  }

  private Map<String, List<MuleSoftFileParser.MuleSoftFlow>> categorizeFlowsByPattern(
      List<MuleSoftFileParser.MuleSoftFlow> flows) {
    Map<String, List<MuleSoftFileParser.MuleSoftFlow>> patterns = new HashMap<>();

    for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
      String pattern = identifyFlowPattern(flow);
      patterns.computeIfAbsent(pattern, k -> new ArrayList<>()).add(flow);
    }

    return patterns;
  }

  private String identifyFlowPattern(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.name == null) {
      return "unknown";
    }

    String name = flow.name.toLowerCase();
    if (name.contains("api") || name.contains("listener") || name.contains("endpoint")) {
      return "api";
    } else if (name.contains("process") || name.contains("transform")) {
      return "processing";
    } else if (name.contains("error") || name.contains("exception")) {
      return "error";
    } else {
      return "other";
    }
  }

  private void checkErrorHandlerConsistency(SensorContext context, InputFile inputFile,
                                             List<MuleSoftFileParser.MuleSoftFlow> flows) {
    int flowsWithErrorHandler = 0;
    int totalFlows = 0;

    for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
      if (!flow.isSubFlow) {
        totalFlows++;
        if (hasErrorHandler(flow)) {
          flowsWithErrorHandler++;
        }
      }
    }

    if (totalFlows > 0 && flowsWithErrorHandler > 0 && flowsWithErrorHandler < totalFlows) {
      double percentage = (double) flowsWithErrorHandler / totalFlows * 100;
      if (percentage < 80 && percentage > 20) {
        reportIssue(context, inputFile,
            "Inconsistent error handler usage across flows (" + flowsWithErrorHandler + " out of " + totalFlows + " flows have error handlers). " +
            "Either use error handlers consistently in all flows or rely on global error handling.");
      }
    }
  }

  private boolean hasErrorHandler(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    org.w3c.dom.NodeList errorHandlers = flow.element.getElementsByTagName("error-handler");
    return errorHandlers.getLength() > 0;
  }

  private void checkLoggerUsageConsistency(SensorContext context, InputFile inputFile,
                                            List<MuleSoftFileParser.MuleSoftFlow> flows,
                                            List<MuleSoftFileParser.LoggerComponent> loggers) {
    Map<String, Integer> loggerCountByFlow = new HashMap<>();

    for (MuleSoftFileParser.LoggerComponent logger : loggers) {
      if (logger.element != null) {
        String flowName = findContainingFlowName(logger.element, flows);
        if (flowName != null) {
          loggerCountByFlow.put(flowName, loggerCountByFlow.getOrDefault(flowName, 0) + 1);
        }
      }
    }

    Set<Integer> loggerCounts = new HashSet<>(loggerCountByFlow.values());
    if (loggerCounts.size() > 3) {
      reportIssue(context, inputFile,
          "Inconsistent logger usage across flows. Some flows have many loggers while others have few. " +
          "Establish a consistent logging strategy.");
    }
  }

  private String findContainingFlowName(org.w3c.dom.Element element,
                                         List<MuleSoftFileParser.MuleSoftFlow> flows) {
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      org.w3c.dom.Element parentElement = (org.w3c.dom.Element) parent;
      String tagName = parentElement.getTagName();
      if ("flow".equals(tagName) || "sub-flow".equals(tagName)) {
        String name = parentElement.getAttribute("name");
        for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
          if (flow.name != null && flow.name.equals(name)) {
            return name;
          }
        }
        return name;
      }
      parent = parent.getParentNode();
    }
    return null;
  }

  private void checkStructuralPatterns(SensorContext context, InputFile inputFile,
                                        Map<String, List<MuleSoftFileParser.MuleSoftFlow>> flowsByPattern) {
    for (Map.Entry<String, List<MuleSoftFileParser.MuleSoftFlow>> entry : flowsByPattern.entrySet()) {
      List<MuleSoftFileParser.MuleSoftFlow> flows = entry.getValue();
      if (flows.size() < 2) {
        continue;
      }

      Map<String, Integer> structureCounts = new HashMap<>();
      for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
        String structure = getFlowStructure(flow);
        structureCounts.put(structure, structureCounts.getOrDefault(structure, 0) + 1);
      }

      if (structureCounts.size() > flows.size() / 2) {
        reportIssue(context, inputFile,
            "Inconsistent structure in " + entry.getKey() + " flows. " +
            "Similar flows should follow similar structural patterns for maintainability.");
      }
    }
  }

  private String getFlowStructure(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return "";
    }

    StringBuilder structure = new StringBuilder();
    org.w3c.dom.NodeList children = flow.element.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        String tagName = child.getNodeName();
        if (!tagName.startsWith("doc:")) {
          structure.append(tagName).append(",");
        }
      }
    }

    return structure.toString();
  }
}
