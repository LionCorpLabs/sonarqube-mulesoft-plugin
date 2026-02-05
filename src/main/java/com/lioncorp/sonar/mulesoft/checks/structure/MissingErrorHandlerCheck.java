package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing error handler.
 */
@Rule(key = "MS037")
public class MissingErrorHandlerCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS037";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      // Only check main flows, not sub-flows (sub-flows typically rely on parent flow error handling)
      if (!flow.isSubFlow && !hasErrorHandler(flow)) {
        reportIssue(context, inputFile,
            "Flow '" + flow.name + "' does not have an error handler. Add error handling to improve reliability.",
            flow.lineNumber);
      }
    }
  }

  private boolean hasErrorHandler(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    // Check for error-handler child element
    org.w3c.dom.NodeList children = flow.element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && "error-handler".equals(child.getNodeName())) {
        return true;
      }
    }
    return false;
  }
}
