package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.regex.Pattern;

/**
 * Hardcoded environment value.
 */
@Rule(key = "MS054")
public class HardcodedEnvironmentValueCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS054";
  }

  private static final Pattern ENV_PATTERN = Pattern.compile(
      "(dev|test|qa|uat|staging|prod|production)", Pattern.CASE_INSENSITIVE);
  private static final Pattern URL_PATTERN = Pattern.compile(
      "https?://[\\w.-]+(dev|test|qa|uat|staging|prod|production)[\\w.-]*", Pattern.CASE_INSENSITIVE);

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check HTTP configurations
    for (MuleSoftFileParser.HttpConfiguration config : parsedFile.httpConfigurations) {
      checkHttpConfig(context, inputFile, config);
    }

    // Check database connectors
    for (MuleSoftFileParser.DatabaseConnector connector : parsedFile.databaseConnectors) {
      checkDatabaseConfig(context, inputFile, connector);
    }

    // Check all elements for hardcoded environment values
    if (parsedFile.xmlDocument != null) {
      checkAllElements(context, inputFile, parsedFile.xmlDocument);
    }
  }

  private void checkHttpConfig(SensorContext context, InputFile inputFile,
                                 MuleSoftFileParser.HttpConfiguration config) {
    if (config.host != null && containsEnvironmentValue(config.host)) {
      reportIssue(context, inputFile,
          "HTTP configuration contains hardcoded environment-specific host '" + config.host + "'. " +
          "Use property placeholders like '${api.host}' instead.");
    }

    if (config.element != null) {
      String url = config.element.getAttribute("url");
      if (url != null && containsEnvironmentValue(url)) {
        reportIssue(context, inputFile,
            "HTTP configuration contains hardcoded environment-specific URL. " +
            "Use property placeholders for environment-specific values.");
      }
    }
  }

  private void checkDatabaseConfig(SensorContext context, InputFile inputFile,
                                     MuleSoftFileParser.DatabaseConnector connector) {
    if (connector.element == null) {
      return;
    }

    String url = connector.element.getAttribute("url");
    String host = connector.element.getAttribute("host");
    String database = connector.element.getAttribute("database");

    if (url != null && containsEnvironmentValue(url)) {
      reportIssue(context, inputFile,
          "Database configuration contains hardcoded environment-specific URL. " +
          "Use property placeholders like '${db.url}' instead.");
    }

    if (host != null && containsEnvironmentValue(host)) {
      reportIssue(context, inputFile,
          "Database configuration contains hardcoded environment-specific host. " +
          "Use property placeholders like '${db.host}' instead.");
    }

    if (database != null && containsEnvironmentValue(database)) {
      reportIssue(context, inputFile,
          "Database configuration contains hardcoded environment-specific database name. " +
          "Use property placeholders like '${db.name}' instead.");
    }
  }

  private void checkAllElements(SensorContext context, InputFile inputFile,
                                  org.w3c.dom.Document document) {
    org.w3c.dom.NodeList allElements = document.getElementsByTagName("*");

    for (int i = 0; i < allElements.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) allElements.item(i);
      checkElementAttributes(context, inputFile, element);
    }
  }

  private void checkElementAttributes(SensorContext context, InputFile inputFile,
                                        org.w3c.dom.Element element) {
    org.w3c.dom.NamedNodeMap attributes = element.getAttributes();

    for (int i = 0; i < attributes.getLength(); i++) {
      org.w3c.dom.Node attr = attributes.item(i);
      String attrName = attr.getNodeName();
      String attrValue = attr.getNodeValue();

      if (attrValue != null && !isPropertyPlaceholder(attrValue) && containsEnvironmentValue(attrValue)) {
        reportIssue(context, inputFile,
            "Element '" + element.getTagName() + "' has hardcoded environment-specific value " +
            "in attribute '" + attrName + "'. Use property placeholders instead.");
      }
    }
  }

  private boolean containsEnvironmentValue(String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }

    return ENV_PATTERN.matcher(value).find() || URL_PATTERN.matcher(value).find();
  }

  private boolean isPropertyPlaceholder(String value) {
    return value.contains("${") || value.contains("#{") || value.contains("p('");
  }
}
