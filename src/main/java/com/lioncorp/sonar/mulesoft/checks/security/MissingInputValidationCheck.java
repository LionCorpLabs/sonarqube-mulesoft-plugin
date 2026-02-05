package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Missing input validation.
 */
@Rule(key = "MS017")
public class MissingInputValidationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS017";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check for flows with http:listener that don't have validation components
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      checkFlowForMissingValidation(context, inputFile, flow);
    }
  }

  private void checkFlowForMissingValidation(SensorContext context, InputFile inputFile, MuleSoftFileParser.MuleSoftFlow flow) {
    Element flowElement = flow.element;
    if (flowElement == null) {
      return;
    }

    // Check if flow contains an HTTP listener
    NodeList httpListeners = flowElement.getElementsByTagNameNS("*", "listener");
    boolean hasHttpListener = false;
    for (int i = 0; i < httpListeners.getLength(); i++) {
      Element listener = (Element) httpListeners.item(i);
      if (isHttpNamespace(listener)) {
        hasHttpListener = true;
        break;
      }
    }

    // If flow has HTTP listener, check for validation components
    if (hasHttpListener) {
      boolean hasValidation = hasValidationComponent(flowElement);
      if (!hasValidation) {
        reportIssue(context, inputFile,
            "Flow '" + flow.name + "' receives HTTP input but lacks input validation. " +
            "Add validation components (validate, set-payload with MEL, etc.) to ensure input safety.",
            flow.lineNumber);
      }
    }
  }

  private boolean hasValidationComponent(Element flowElement) {
    // Check for various validation patterns
    // 1. validate element
    NodeList validations = flowElement.getElementsByTagName("validate");
    if (validations.getLength() > 0) {
      return true;
    }

    // 2. set-payload with MEL expressions for validation
    NodeList setPayloads = flowElement.getElementsByTagName("set-payload");
    for (int i = 0; i < setPayloads.getLength(); i++) {
      Element payload = (Element) setPayloads.item(i);
      String value = payload.getAttribute("value");
      if (value != null && (value.contains("validate") || value.contains("matches") || value.contains("is"))) {
        return true;
      }
    }

    // 3. Choice router with when conditions (for branching validation)
    NodeList choices = flowElement.getElementsByTagName("choice");
    if (choices.getLength() > 0) {
      return true;
    }

    // 4. Custom validators
    NodeList components = flowElement.getElementsByTagName("component");
    for (int i = 0; i < components.getLength(); i++) {
      Element component = (Element) components.item(i);
      String className = component.getAttribute("class");
      if (className != null && className.toLowerCase().contains("validat")) {
        return true;
      }
    }

    return false;
  }

  private boolean isHttpNamespace(Element element) {
    String namespaceURI = element.getNamespaceURI();
    return namespaceURI != null && namespaceURI.contains("http");
  }
}
