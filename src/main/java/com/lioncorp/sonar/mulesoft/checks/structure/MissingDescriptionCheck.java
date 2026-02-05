package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing description.
 */
@Rule(key = "MS057")
public class MissingDescriptionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS057";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (!hasDescription(flow)) {
        String flowType = flow.isSubFlow ? "Sub-flow" : "Flow";
        reportIssue(context, inputFile,
            flowType + " '" + flow.name + "' is missing a doc:description element. " +
            "Add documentation to explain the purpose and behavior of this flow.");
      }
    }
  }

  private boolean hasDescription(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    // Check for doc:description element
    org.w3c.dom.NodeList children = flow.element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        String nodeName = child.getNodeName();
        if ("doc:description".equals(nodeName) || nodeName.endsWith(":description")) {
          // Check if description has content
          String content = child.getTextContent();
          if (content != null && !content.trim().isEmpty()) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
