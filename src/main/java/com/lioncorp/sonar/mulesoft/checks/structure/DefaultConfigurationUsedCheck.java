package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.HashSet;
import java.util.Set;

/**
 * Default configuration used.
 */
@Rule(key = "MS056")
public class DefaultConfigurationUsedCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS056";
  }

  private static final Set<String> DEFAULT_VALUES = new HashSet<>();
  static {
    DEFAULT_VALUES.add("localhost");
    DEFAULT_VALUES.add("8081");
    DEFAULT_VALUES.add("8082");
    DEFAULT_VALUES.add("admin");
    DEFAULT_VALUES.add("password");
    DEFAULT_VALUES.add("root");
    DEFAULT_VALUES.add("test");
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP configurations
    for (MuleSoftFileParser.HttpConfiguration config : parsedFile.httpConfigurations) {
      checkHttpConfiguration(context, inputFile, config);
    }

    // Check database connectors
    for (MuleSoftFileParser.DatabaseConnector connector : parsedFile.databaseConnectors) {
      checkDatabaseConfiguration(context, inputFile, connector);
    }

    // Check all configuration elements
    if (parsedFile.xmlDocument != null) {
      checkAllConfigurations(context, inputFile, parsedFile.xmlDocument);
    }
  }

  private void checkHttpConfiguration(SensorContext context, InputFile inputFile,
                                       MuleSoftFileParser.HttpConfiguration config) {
    if (config.host != null && DEFAULT_VALUES.contains(config.host)) {
      reportIssue(context, inputFile,
          "HTTP configuration uses default host '" + config.host + "'. " +
          "Use externalized configuration properties instead.");
    }

    if (config.port != null && DEFAULT_VALUES.contains(config.port)) {
      reportIssue(context, inputFile,
          "HTTP configuration uses default port '" + config.port + "'. " +
          "Use externalized configuration properties instead.");
    }
  }

  private void checkDatabaseConfiguration(SensorContext context, InputFile inputFile,
                                           MuleSoftFileParser.DatabaseConnector connector) {
    if (connector.element == null) {
      return;
    }

    String url = connector.element.getAttribute("url");
    String user = connector.element.getAttribute("user");
    String host = connector.element.getAttribute("host");

    if (url != null && (url.contains("localhost") || url.contains("127.0.0.1"))) {
      reportIssue(context, inputFile,
          "Database configuration uses localhost in URL. " +
          "Use externalized configuration properties instead.");
    }

    if (user != null && DEFAULT_VALUES.contains(user)) {
      reportIssue(context, inputFile,
          "Database configuration uses default user '" + user + "'. " +
          "Use externalized configuration properties instead.");
    }

    if (host != null && DEFAULT_VALUES.contains(host)) {
      reportIssue(context, inputFile,
          "Database configuration uses default host '" + host + "'. " +
          "Use externalized configuration properties instead.");
    }
  }

  private void checkAllConfigurations(SensorContext context, InputFile inputFile,
                                       org.w3c.dom.Document document) {
    org.w3c.dom.NodeList configNodes = document.getElementsByTagNameNS("*", "config");
    for (int i = 0; i < configNodes.getLength(); i++) {
      org.w3c.dom.Element configElement = (org.w3c.dom.Element) configNodes.item(i);
      checkConfigElement(context, inputFile, configElement);
    }
  }

  private void checkConfigElement(SensorContext context, InputFile inputFile,
                                    org.w3c.dom.Element element) {
    org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      org.w3c.dom.Node attr = attributes.item(i);
      String value = attr.getNodeValue();

      if (value != null && DEFAULT_VALUES.contains(value.toLowerCase())) {
        reportIssue(context, inputFile,
            "Configuration element '" + element.getTagName() + "' uses default value '" + value + "' " +
            "in attribute '" + attr.getNodeName() + "'. Use externalized configuration properties instead.");
      }
    }
  }
}
