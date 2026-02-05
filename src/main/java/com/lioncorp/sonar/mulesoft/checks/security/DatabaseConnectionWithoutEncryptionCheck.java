package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Database connection without SSL.
 */
@Rule(key = "MS021")
public class DatabaseConnectionWithoutEncryptionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS021";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check database connectors for missing SSL/TLS encryption
    for (MuleSoftFileParser.DatabaseConnector connector : parsedFile.databaseConnectors) {
      if (!connector.hasSSL && connector.element != null) {
        String url = connector.element.getAttribute("url");
        if (url != null && !hasEncryption(url)) {
          reportIssue(context, inputFile,
              "Database connection without SSL/TLS encryption detected. " +
              "Enable SSL/TLS for database connections to protect data in transit (add useSSL=true, sslMode=require, or ssl=true to connection URL).");
        }
      }
    }
  }

  private boolean hasEncryption(String url) {
    if (url == null || url.isEmpty()) {
      return false;
    }

    String lowerUrl = url.toLowerCase();

    // Check for SSL/TLS parameters in connection URL
    return lowerUrl.contains("usessl=true") ||
           lowerUrl.contains("ssl=true") ||
           lowerUrl.contains("sslmode=require") ||
           lowerUrl.contains("sslmode=verify") ||
           lowerUrl.contains("encrypt=true") ||
           lowerUrl.contains("trustservercertificate=false");
  }
}
