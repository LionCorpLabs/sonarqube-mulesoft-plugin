package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing API documentation.
 */
@Rule(key = "MS068")
public class MissingAPIDocumentationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS068";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP listeners (API endpoints)
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("listener".equals(httpConfig.type)) {
        // Find the flow containing this listener
        String flowName = findContainingFlowName(httpConfig.element, parsedFile.flows);
        if (flowName != null) {
          MuleSoftFileParser.MuleSoftFlow flow = findFlowByName(parsedFile.flows, flowName);
          if (flow != null && !hasDocumentation(flow)) {
            reportIssue(context, inputFile,
                "API endpoint in flow '" + flowName + "' lacks documentation. Add doc:description to explain the endpoint's purpose, parameters, and responses.");
          }
        }
      }
    }
  }

  private String findContainingFlowName(org.w3c.dom.Element element, java.util.List<MuleSoftFileParser.MuleSoftFlow> flows) {
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      org.w3c.dom.Element parentElement = (org.w3c.dom.Element) parent;
      String tagName = parentElement.getTagName();
      if ("flow".equals(tagName) || "sub-flow".equals(tagName)) {
        String flowName = parentElement.getAttribute("name");
        // Verify this flow exists in our list
        if (findFlowByName(flows, flowName) != null) {
          return flowName;
        }
      }
      parent = parent.getParentNode();
    }
    return null;
  }

  private MuleSoftFileParser.MuleSoftFlow findFlowByName(java.util.List<MuleSoftFileParser.MuleSoftFlow> flows, String name) {
    for (MuleSoftFileParser.MuleSoftFlow flow : flows) {
      if (name.equals(flow.name)) {
        return flow;
      }
    }
    return null;
  }

  private boolean hasDocumentation(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element != null) {
      org.w3c.dom.NodeList docNodes = flow.element.getElementsByTagName("doc:description");
      return docNodes.getLength() > 0;
    }
    return false;
  }
}
