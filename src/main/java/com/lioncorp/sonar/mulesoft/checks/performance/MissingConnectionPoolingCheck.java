package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing connection pooling.
 */
@Rule(key = "MS081")
public class MissingConnectionPoolingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS081";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check database configurations for pooling
    checkDatabasePooling(context, inputFile, parsedFile);

    // Check HTTP request configurations for pooling
    checkHTTPPooling(context, inputFile, parsedFile);

    // Check JMS configurations for pooling
    checkJMSPooling(context, inputFile, parsedFile);

    // Check FTP/SFTP configurations for pooling
    checkFTPPooling(context, inputFile, parsedFile);
  }

  private void checkDatabasePooling(SensorContext context, InputFile inputFile,
                                   MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      if ("config".equals(dbConnector.type)) {
        if (!dbConnector.hasPooling) {
          String configName = dbConnector.element.getAttribute("name");
          reportIssue(context, inputFile,
              String.format("Database configuration '%s' is missing connection pooling. " +
                  "Add pooling-profile to improve performance and resource utilization.",
                  configName.isEmpty() ? "unnamed" : configName));
        } else {
          // Check pooling configuration values
          checkPoolingValues(context, inputFile, dbConnector.element);
        }
      }
    }
  }

  private void checkHTTPPooling(SensorContext context, InputFile inputFile,
                               MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.HttpConfiguration httpConfig : parsedFile.httpConfigurations) {
      if ("request-config".equals(httpConfig.type)) {
        if (!hasPoolingProfile(httpConfig.element)) {
          String configName = httpConfig.element.getAttribute("name");
          reportIssue(context, inputFile,
              String.format("HTTP request configuration '%s' is missing connection pooling. " +
                  "Consider adding connection pooling for better performance with multiple requests.",
                  configName.isEmpty() ? "unnamed" : configName));
        }
      }
    }
  }

  private void checkJMSPooling(SensorContext context, InputFile inputFile,
                              MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    org.w3c.dom.NodeList jmsConfigNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "config");

    for (int i = 0; i < jmsConfigNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) jmsConfigNodes.item(i);
      String namespaceURI = element.getNamespaceURI();

      if (namespaceURI != null && namespaceURI.contains("jms")) {
        if (!hasPoolingProfile(element)) {
          String configName = element.getAttribute("name");
          reportIssue(context, inputFile,
              String.format("JMS configuration '%s' is missing connection pooling. " +
                  "Add caching-connection-factory to improve performance.",
                  configName.isEmpty() ? "unnamed" : configName));
        }
      }
    }
  }

  private void checkFTPPooling(SensorContext context, InputFile inputFile,
                              MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    String[] ftpNamespaces = {"ftp", "sftp", "ftps"};

    for (String namespace : ftpNamespaces) {
      org.w3c.dom.NodeList ftpConfigNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "config");

      for (int i = 0; i < ftpConfigNodes.getLength(); i++) {
        org.w3c.dom.Element element = (org.w3c.dom.Element) ftpConfigNodes.item(i);
        String namespaceURI = element.getNamespaceURI();

        if (namespaceURI != null && namespaceURI.contains(namespace)) {
          if (!hasPoolingProfile(element)) {
            String configName = element.getAttribute("name");
            reportIssue(context, inputFile,
                String.format("%s configuration '%s' is missing connection pooling. " +
                    "Add pooling profile to reuse connections efficiently.",
                    namespace.toUpperCase(), configName.isEmpty() ? "unnamed" : configName));
          }
        }
      }
    }
  }

  private boolean hasPoolingProfile(org.w3c.dom.Element element) {
    // Check for pooling-profile child element
    org.w3c.dom.NodeList poolingNodes = element.getElementsByTagName("pooling-profile");
    if (poolingNodes.getLength() > 0) {
      return true;
    }

    // Check for poolingProfile attribute
    if (element.hasAttribute("poolingProfile")) {
      return true;
    }

    // Check for connection-pooling-profile
    org.w3c.dom.NodeList connectionPoolingNodes = element.getElementsByTagName("connection-pooling-profile");
    if (connectionPoolingNodes.getLength() > 0) {
      return true;
    }

    // Check for caching-connection-factory (JMS)
    org.w3c.dom.NodeList cachingNodes = element.getElementsByTagName("caching-connection-factory");
    return cachingNodes.getLength() > 0;
  }

  private void checkPoolingValues(SensorContext context, InputFile inputFile, org.w3c.dom.Element dbElement) {
    org.w3c.dom.NodeList poolingNodes = dbElement.getElementsByTagName("pooling-profile");

    if (poolingNodes.getLength() > 0) {
      org.w3c.dom.Element poolingElement = (org.w3c.dom.Element) poolingNodes.item(0);

      // Check maxPoolSize
      String maxPoolSize = poolingElement.getAttribute("maxPoolSize");
      if (maxPoolSize.isEmpty()) {
        reportIssue(context, inputFile,
            "Pooling profile is missing maxPoolSize attribute. " +
            "Define appropriate pool size limits to prevent resource exhaustion.");
      }

      // Check minPoolSize
      String minPoolSize = poolingElement.getAttribute("minPoolSize");
      if (!minPoolSize.isEmpty() && !maxPoolSize.isEmpty()) {
        try {
          int min = Integer.parseInt(minPoolSize);
          int max = Integer.parseInt(maxPoolSize);

          if (min >= max) {
            reportIssue(context, inputFile,
                "Pooling profile minPoolSize is greater than or equal to maxPoolSize. " +
                "This can cause connection pool issues.");
          }
        } catch (NumberFormatException e) {
          // Ignore if values are expressions
        }
      }

      // Check for connection timeout
      String maxWait = poolingElement.getAttribute("maxWait");
      if (maxWait.isEmpty()) {
        reportIssue(context, inputFile,
            "Pooling profile is missing maxWait attribute. " +
            "Define connection timeout to prevent indefinite waiting.");
      }
    }
  }
}
