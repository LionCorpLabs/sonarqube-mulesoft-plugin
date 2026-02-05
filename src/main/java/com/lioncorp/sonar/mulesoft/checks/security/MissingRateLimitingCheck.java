package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing rate limiting.
 */
@Rule(key = "MS022")
public class MissingRateLimitingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS022";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows with HTTP listeners for missing rate limiting
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (hasHttpListener(flow) && !hasRateLimiting(flow)) {
        reportIssue(context, inputFile,
            "Flow '" + flow.name + "' exposes HTTP endpoint without rate limiting. " +
            "Implement rate limiting, throttling, or spike control policies to prevent DoS attacks and abuse.");
      }
    }
  }

  private boolean hasHttpListener(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    org.w3c.dom.NodeList listeners = flow.element.getElementsByTagNameNS("*", "listener");
    for (int i = 0; i < listeners.getLength(); i++) {
      org.w3c.dom.Element listener = (org.w3c.dom.Element) listeners.item(i);
      String namespaceURI = listener.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("http")) {
        return true;
      }
    }

    return false;
  }

  private boolean hasRateLimiting(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    String flowContent = flow.element.getTextContent();
    if (flowContent == null) {
      return false;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for rate limiting patterns
    return lowerContent.contains("rate-limit") ||
           lowerContent.contains("ratelimit") ||
           lowerContent.contains("throttle") ||
           lowerContent.contains("throttling") ||
           lowerContent.contains("spike") ||
           lowerContent.contains("quota") ||
           lowerContent.contains("rate_limit");
  }
}
