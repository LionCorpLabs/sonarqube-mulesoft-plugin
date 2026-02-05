package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.constants.CheckConstants;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.SecurityPatterns;
import com.lioncorp.sonar.mulesoft.utils.StringUtils;
import com.lioncorp.sonar.mulesoft.utils.XmlUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Cleartext protocol used.
 */
@Rule(key = "MS012")
public class ClearTextProtocolCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS012";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP configurations for cleartext protocols
    for (MuleSoftFileParser.HttpConfiguration config : parsedFile.httpConfigurations) {
      if (config.protocol != null && SecurityPatterns.isCleartextProtocol(config.protocol)) {
        reportIssue(context, inputFile,
            "Cleartext protocol '" + config.protocol + "' detected. Use encrypted protocols (HTTPS, FTPS, etc.) to protect data in transit.");
      }
    }

    // Check database connectors for cleartext connections
    for (MuleSoftFileParser.DatabaseConnector connector : parsedFile.databaseConnectors) {
      if (connector.element != null) {
        String url = XmlUtils.getAttributeValue(connector.element, "url");
        if (!StringUtils.isNullOrEmpty(url) && CheckConstants.isCleartextProtocol(url)) {
          reportIssue(context, inputFile,
              "Cleartext protocol detected in database connection URL. Use SSL/TLS encryption for database connections.");
        }
      }
    }

    // Check for cleartext protocols in raw content
    if (parsedFile.xmlDocument != null) {
      XmlUtils.visitElements(parsedFile.xmlDocument.getDocumentElement(), element -> {
        String[] attributesToCheck = {"url", "host", "protocol", "address", "endpoint", "baseUri"};
        for (String attrName : attributesToCheck) {
          String attrValue = XmlUtils.getAttributeValue(element, attrName);
          if (!StringUtils.isNullOrEmpty(attrValue) && CheckConstants.isCleartextProtocol(attrValue)) {
            reportIssue(context, inputFile,
                "Cleartext protocol detected in '" + attrName + "' attribute. Use encrypted protocols (HTTPS, FTPS, SMTPS, etc.) instead.");
            break; // Only report once per element
          }
        }
      });
    }
  }
}
