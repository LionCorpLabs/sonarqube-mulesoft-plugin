package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Excessive logging.
 */
@Rule(key = "MS075")
public class ExcessiveLoggingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS075";
  }
  private static final int MAX_LOGGERS_PER_FLOW = 10;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check each flow for excessive logging
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      int loggerCount = countLoggersInElement(flow.element);

      if (loggerCount > MAX_LOGGERS_PER_FLOW) {
        reportIssue(context, inputFile,
            String.format("Flow '%s' contains %d logger statements (maximum recommended: %d). " +
                "Excessive logging can impact performance. Consider reducing log verbosity or using async logging.",
                flow.name, loggerCount, MAX_LOGGERS_PER_FLOW));
      }
    }
  }

  private int countLoggersInElement(org.w3c.dom.Element element) {
    int count = 0;

    // Count logger elements in this element and its children
    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        org.w3c.dom.Element childElement = (org.w3c.dom.Element) child;

        // Check if this is a logger element
        if ("logger".equals(childElement.getLocalName())) {
          count++;
        }

        // Recursively count in child elements
        count += countLoggersInElement(childElement);
      }
    }

    return count;
  }
}
