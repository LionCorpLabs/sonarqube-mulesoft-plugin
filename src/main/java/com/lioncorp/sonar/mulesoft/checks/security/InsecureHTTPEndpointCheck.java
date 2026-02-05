package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.SecurityPatterns;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;

/**
 * Check for HTTP endpoints without TLS/SSL configuration.
 */
@Rule(key = "MS002")
public class InsecureHTTPEndpointCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS002";
  }

  private static final Logger LOG = Loggers.get(InsecureHTTPEndpointCheck.class);

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    LOG.debug("Checking file {} for insecure HTTP endpoints. Found {} HTTP configurations.", inputFile.filename(), parsedFile.httpConfigurations.size());

    // Track reported line numbers to avoid duplicate issues
    java.util.Set<Integer> reportedLines = new java.util.HashSet<>();

    for (MuleSoftFileParser.HttpConfiguration config : parsedFile.httpConfigurations) {
      LOG.debug("HTTP config type={}, protocol={}, host={}, hasTLS={}, lineNumber={}",
                config.type, config.protocol, config.host, config.hasTLS, config.lineNumber);
      if (isInsecureHttp(config)) {
        // Only report if we haven't already reported this line
        if (!reportedLines.contains(config.lineNumber)) {
          LOG.info("Found insecure HTTP endpoint in {} at line {}: protocol={}",
                   inputFile.filename(), config.lineNumber, config.protocol);
          reportIssue(context, inputFile, "Insecure HTTP endpoint detected. Use HTTPS/TLS for secure communication.", config.lineNumber);
          reportedLines.add(config.lineNumber);
        } else {
          LOG.debug("Skipping duplicate issue at line {}", config.lineNumber);
        }
      }
    }
  }

  private boolean isInsecureHttp(MuleSoftFileParser.HttpConfiguration config) {
    // Check if protocol is explicitly HTTP (not HTTPS)
    if ("HTTP".equalsIgnoreCase(config.protocol)) {
      return true;
    }

    // If no protocol specified and no TLS context, it's insecure
    if ((config.protocol == null || config.protocol.isEmpty()) && !config.hasTLS) {
      return true;
    }

    return false;
  }
}
