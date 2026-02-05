package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * HTTP request without authentication.
 */
@Rule(key = "MS009")
public class MissingAuthenticationHeaderCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS009";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP configurations for missing authentication
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request".equals(httpConfig.type) && !httpConfig.hasAuthentication) {
        // Check if it's an external API call (not localhost)
        String host = httpConfig.host;
        if (host != null && !host.isEmpty() && !isLocalhost(host)) {
          reportIssue(context, inputFile, "HTTP request to external endpoint without authentication. Add authentication headers or use secure authentication.");
        }
      }
    }
  }

  private boolean isLocalhost(String host) {
    return host.contains("localhost") ||
           host.contains("127.0.0.1") ||
           host.contains("::1") ||
           host.contains("${") ||  // Property placeholder
           host.contains("#{");    // MEL expression
  }
}
