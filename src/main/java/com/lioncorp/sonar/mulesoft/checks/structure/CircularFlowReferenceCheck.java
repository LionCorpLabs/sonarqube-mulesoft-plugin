package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.*;

/**
 * Circular flow reference.
 */
@Rule(key = "MS036")
public class CircularFlowReferenceCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS036";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Build a graph of flow references
    Map<String, List<String>> flowGraph = buildFlowGraph(parsedFile);

    // Find circular dependencies using DFS
    Set<String> visited = new HashSet<>();
    Set<String> recursionStack = new HashSet<>();

    for (String flowName : flowGraph.keySet()) {
      if (!visited.contains(flowName)) {
        List<String> cycle = detectCycle(flowName, flowGraph, visited, recursionStack, new ArrayList<>());
        if (cycle != null && !cycle.isEmpty()) {
          reportIssue(context, inputFile,
              "Circular flow reference detected: " + String.join(" -> ", cycle));
        }
      }
    }
  }

  private Map<String, List<String>> buildFlowGraph(MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    Map<String, List<String>> graph = new HashMap<>();

    // Initialize all flows in the graph
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (flow.name != null && !flow.name.isEmpty()) {
        graph.put(flow.name, new ArrayList<>());
      }
    }

    // Add edges for flow references
    for (MuleSoftFileParser.FlowReference ref : parsedFile.flowReferences) {
      if (ref.sourceFlowName != null && ref.flowName != null) {
        graph.computeIfAbsent(ref.sourceFlowName, k -> new ArrayList<>()).add(ref.flowName);
      }
    }

    return graph;
  }

  private List<String> detectCycle(String flowName, Map<String, List<String>> graph,
                                    Set<String> visited, Set<String> recursionStack,
                                    List<String> path) {
    visited.add(flowName);
    recursionStack.add(flowName);
    path.add(flowName);

    List<String> references = graph.get(flowName);
    if (references != null) {
      for (String refFlow : references) {
        if (!visited.contains(refFlow)) {
          List<String> cycle = detectCycle(refFlow, graph, visited, recursionStack, new ArrayList<>(path));
          if (cycle != null) {
            return cycle;
          }
        } else if (recursionStack.contains(refFlow)) {
          // Found a cycle - build the cycle path
          List<String> cycle = new ArrayList<>(path);
          cycle.add(refFlow);
          // Trim to just the cycle part
          int cycleStart = cycle.indexOf(refFlow);
          return cycle.subList(cycleStart, cycle.size());
        }
      }
    }

    recursionStack.remove(flowName);
    return Collections.emptyList();
  }
}
