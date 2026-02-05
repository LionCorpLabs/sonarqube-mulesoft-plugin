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
 * Mixed responsibility in flow.
 */
@Rule(key = "MS040")
public class MixedResponsibilityFlowCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS040";
  }
  private static final int MIN_RESPONSIBILITY_TYPES = 3;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      Set<String> responsibilities = identifyResponsibilities(flow);
      if (responsibilities.size() >= MIN_RESPONSIBILITY_TYPES) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' has mixed responsibilities: " +
            String.join(", ", responsibilities) + ". " +
            "Consider splitting into separate flows with single, clear purposes.");
      }
    }
  }

  private Set<String> identifyResponsibilities(MuleSoftFileParser.MuleSoftFlow flow) {
    Set<String> responsibilities = new HashSet<>();

    if (flow.element == null) {
      return responsibilities;
    }

    // Check for different types of operations
    NodeList children = flow.element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element) {
        Element child = (Element) children.item(i);
        String nodeName = child.getNodeName();

        // HTTP operations
        if (nodeName.contains("http:") || nodeName.contains("listener") || nodeName.contains("request")) {
          responsibilities.add("HTTP operations");
        }

        // Database operations
        if (nodeName.contains("db:") || nodeName.contains("database")) {
          responsibilities.add("database operations");
        }

        // File operations
        if (nodeName.contains("file:") || nodeName.contains("sftp:") || nodeName.contains("ftp:")) {
          responsibilities.add("file operations");
        }

        // Transformation operations
        if (nodeName.contains("transform") || nodeName.contains("dataweave") || nodeName.contains("set-payload")) {
          responsibilities.add("data transformation");
        }

        // Logging operations
        if (nodeName.equals("logger")) {
          responsibilities.add("logging");
        }

        // Validation operations
        if (nodeName.contains("validate") || nodeName.contains("validation")) {
          responsibilities.add("validation");
        }

        // Message queue operations
        if (nodeName.contains("jms:") || nodeName.contains("amqp:") || nodeName.contains("vm:")) {
          responsibilities.add("message queue operations");
        }

        // Business logic (Java calls)
        if (nodeName.contains("java:") || nodeName.contains("invoke")) {
          responsibilities.add("business logic");
        }
      }
    }

    return responsibilities;
  }
}
