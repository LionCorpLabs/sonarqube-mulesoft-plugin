package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.StringUtils;
import com.lioncorp.sonar.mulesoft.utils.XmlUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Hardcoded IP address in configuration.
 */
@Rule(key = "MS011")
public class HardcodedIPAddressCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS011";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP configurations for hardcoded IP addresses
    for (MuleSoftFileParser.HttpConfiguration config : parsedFile.httpConfigurations) {
      if (config.host != null && isIPAddress(config.host) && !isLocalhost(config.host)) {
        reportIssue(context, inputFile,
            "Hardcoded IP address '" + config.host + "' detected in HTTP configuration. Use property placeholders or DNS names for better maintainability and portability.");
      }
    }

    // Check database connectors for hardcoded IP addresses
    for (MuleSoftFileParser.DatabaseConnector connector : parsedFile.databaseConnectors) {
      if (connector.element != null) {
        String url = XmlUtils.getAttributeValue(connector.element, "url");
        if (!StringUtils.isNullOrEmpty(url) && containsIPAddress(url)) {
          reportIssue(context, inputFile,
              "Hardcoded IP address detected in database connection URL. Use property placeholders or DNS names instead.");
        }
      }
    }

    // Check raw content for IP addresses in attributes
    if (parsedFile.xmlDocument != null) {
      XmlUtils.visitElements(parsedFile.xmlDocument.getDocumentElement(), element -> {
        String[] attributesToCheck = {"host", "url", "address", "endpoint", "baseUri", "basePath"};
        for (String attrName : attributesToCheck) {
          String attrValue = XmlUtils.getAttributeValue(element, attrName);
          if (!StringUtils.isNullOrEmpty(attrValue) && containsIPAddress(attrValue) && !isLocalhost(attrValue)) {
            reportIssue(context, inputFile,
                "Hardcoded IP address detected in '" + attrName + "' attribute. Use property placeholders or DNS names for better portability.");
            break; // Only report once per element
          }
        }
      });
    }
  }

  private boolean isIPAddress(String host) {
    if (StringUtils.isNullOrEmpty(host)) {
      return false;
    }
    // Simple IPv4 pattern check
    String ipv4Pattern = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$";
    return host.matches(ipv4Pattern);
  }

  private boolean containsIPAddress(String value) {
    if (StringUtils.isNullOrEmpty(value)) {
      return false;
    }
    // Check for IPv4 addresses in the string
    String ipv4Pattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
    return value.matches(".*" + ipv4Pattern + ".*");
  }

  private boolean isLocalhost(String host) {
    if (host == null) {
      return false;
    }
    return host.equals("127.0.0.1") || host.equals("0.0.0.0") ||
           host.equals("localhost") || host.startsWith("127.") ||
           host.equals("::1");
  }
}
