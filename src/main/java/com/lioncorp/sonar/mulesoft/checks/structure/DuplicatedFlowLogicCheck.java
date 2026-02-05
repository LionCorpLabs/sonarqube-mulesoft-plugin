package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.*;

/**
 * Duplicated flow logic.
 */
@Rule(key = "MS033")
public class DuplicatedFlowLogicCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS033";
  }
  private static final double SIMILARITY_THRESHOLD = 0.8;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    Map<String, List<MuleSoftFileParser.MuleSoftFlow>> flowsByStructure = new HashMap<>();

    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      String structure = buildFlowStructure(flow);
      if (structure != null && !structure.isEmpty()) {
        flowsByStructure.computeIfAbsent(structure, k -> new ArrayList<>()).add(flow);
      }
    }

    for (Map.Entry<String, List<MuleSoftFileParser.MuleSoftFlow>> entry : flowsByStructure.entrySet()) {
      if (entry.getValue().size() > 1) {
        List<String> flowNames = new ArrayList<>();
        for (MuleSoftFileParser.MuleSoftFlow flow : entry.getValue()) {
          flowNames.add(flow.name);
        }
        reportIssue(context, inputFile,
            "Duplicated flow logic detected in flows: " + String.join(", ", flowNames) + ". " +
            "Consider extracting common logic into a reusable sub-flow.");
      }
    }

    checkSimilarFlows(context, inputFile, parsedFile.flows);
  }

  private String buildFlowStructure(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return "";
    }

    StringBuilder structure = new StringBuilder();
    org.w3c.dom.NodeList children = flow.element.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        String tagName = child.getNodeName();
        if (!tagName.equals("error-handler") && !tagName.startsWith("doc:")) {
          structure.append(tagName).append(";");
        }
      }
    }

    return structure.toString();
  }

  private void checkSimilarFlows(SensorContext context, InputFile inputFile,
                                  List<MuleSoftFileParser.MuleSoftFlow> flows) {
    for (int i = 0; i < flows.size(); i++) {
      for (int j = i + 1; j < flows.size(); j++) {
        MuleSoftFileParser.MuleSoftFlow flow1 = flows.get(i);
        MuleSoftFileParser.MuleSoftFlow flow2 = flows.get(j);

        double similarity = calculateSimilarity(flow1, flow2);
        if (similarity >= SIMILARITY_THRESHOLD) {
          reportIssue(context, inputFile,
              "Flows '" + flow1.name + "' and '" + flow2.name + "' have " +
              String.format("%.0f%%", similarity * 100) + " similar structure. " +
              "Consider extracting common logic into a reusable sub-flow.");
        }
      }
    }
  }

  private double calculateSimilarity(MuleSoftFileParser.MuleSoftFlow flow1,
                                      MuleSoftFileParser.MuleSoftFlow flow2) {
    List<String> components1 = getFlowComponents(flow1);
    List<String> components2 = getFlowComponents(flow2);

    if (components1.isEmpty() || components2.isEmpty()) {
      return 0.0;
    }

    int matchCount = 0;
    int minSize = Math.min(components1.size(), components2.size());

    for (int i = 0; i < minSize; i++) {
      if (components1.get(i).equals(components2.get(i))) {
        matchCount++;
      }
    }

    int maxSize = Math.max(components1.size(), components2.size());
    return (double) matchCount / maxSize;
  }

  private List<String> getFlowComponents(MuleSoftFileParser.MuleSoftFlow flow) {
    List<String> components = new ArrayList<>();
    if (flow.element == null) {
      return components;
    }

    org.w3c.dom.NodeList children = flow.element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        String tagName = child.getNodeName();
        if (!tagName.equals("error-handler") && !tagName.startsWith("doc:")) {
          components.add(tagName);
        }
      }
    }

    return components;
  }
}
