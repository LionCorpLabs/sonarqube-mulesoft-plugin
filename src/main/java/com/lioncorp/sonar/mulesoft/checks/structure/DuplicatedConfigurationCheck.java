package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.*;

/**
 * Duplicated configuration.
 */
@Rule(key = "MS053")
public class DuplicatedConfigurationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS053";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check for duplicate HTTP configurations
    checkDuplicateHttpConfigurations(context, inputFile, parsedFile.httpConfigurations);

    // Check for duplicate database configurations
    checkDuplicateDatabaseConfigurations(context, inputFile, parsedFile.databaseConnectors);

    // Check for duplicate configuration values across all elements
    if (parsedFile.xmlDocument != null) {
      checkDuplicateConfigurationValues(context, inputFile, parsedFile.xmlDocument);
    }
  }

  private void checkDuplicateHttpConfigurations(SensorContext context, InputFile inputFile,
                                                  List<MuleSoftFileParser.HttpConfiguration> configs) {
    Map<String, List<MuleSoftFileParser.HttpConfiguration>> configsByKey = new HashMap<>();

    for (MuleSoftFileParser.HttpConfiguration config : configs) {
      String key = buildHttpConfigKey(config);
      configsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(config);
    }

    for (Map.Entry<String, List<MuleSoftFileParser.HttpConfiguration>> entry : configsByKey.entrySet()) {
      if (entry.getValue().size() > 1) {
        reportIssue(context, inputFile,
            "Duplicated HTTP configuration detected (" + entry.getValue().size() + " occurrences). " +
            "Extract to a shared configuration element to follow DRY principle.");
      }
    }
  }

  private String buildHttpConfigKey(MuleSoftFileParser.HttpConfiguration config) {
    StringBuilder key = new StringBuilder();
    if (config.protocol != null) {
      key.append(config.protocol).append(":");
    }
    if (config.host != null) {
      key.append(config.host).append(":");
    }
    if (config.port != null) {
      key.append(config.port);
    }
    return key.toString();
  }

  private void checkDuplicateDatabaseConfigurations(SensorContext context, InputFile inputFile,
                                                      List<MuleSoftFileParser.DatabaseConnector> connectors) {
    Map<String, Integer> configCounts = new HashMap<>();

    for (MuleSoftFileParser.DatabaseConnector connector : connectors) {
      if (connector.element == null || !connector.type.equals("config")) {
        continue;
      }

      String url = connector.element.getAttribute("url");
      if (url != null && !url.isEmpty()) {
        configCounts.put(url, configCounts.getOrDefault(url, 0) + 1);
      }
    }

    for (Map.Entry<String, Integer> entry : configCounts.entrySet()) {
      if (entry.getValue() > 1) {
        reportIssue(context, inputFile,
            "Duplicated database configuration with same URL (" + entry.getValue() + " occurrences). " +
            "Reuse a single configuration element.");
      }
    }
  }

  private void checkDuplicateConfigurationValues(SensorContext context, InputFile inputFile,
                                                   org.w3c.dom.Document document) {
    Map<String, List<String>> valuesByType = new HashMap<>();

    org.w3c.dom.NodeList allElements = document.getElementsByTagName("*");
    for (int i = 0; i < allElements.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) allElements.item(i);
      String tagName = element.getTagName();

      if (tagName.endsWith("-config") || tagName.equals("configuration")) {
        String configSignature = buildConfigSignature(element);
        if (!configSignature.isEmpty()) {
          valuesByType.computeIfAbsent(tagName, k -> new ArrayList<>()).add(configSignature);
        }
      }
    }

    for (Map.Entry<String, List<String>> entry : valuesByType.entrySet()) {
      Map<String, Long> duplicateCounts = entry.getValue().stream()
          .collect(java.util.stream.Collectors.groupingBy(s -> s, java.util.stream.Collectors.counting()));

      for (Map.Entry<String, Long> dupEntry : duplicateCounts.entrySet()) {
        if (dupEntry.getValue() > 1) {
          reportIssue(context, inputFile,
              "Duplicated configuration '" + entry.getKey() + "' detected (" + dupEntry.getValue() + " occurrences). " +
              "Extract to a reusable global configuration element.");
        }
      }
    }
  }

  private String buildConfigSignature(org.w3c.dom.Element element) {
    StringBuilder signature = new StringBuilder();
    org.w3c.dom.NamedNodeMap attributes = element.getAttributes();

    List<String> attrPairs = new ArrayList<>();
    for (int i = 0; i < attributes.getLength(); i++) {
      org.w3c.dom.Node attr = attributes.item(i);
      String name = attr.getNodeName();
      if (!name.equals("name") && !name.equals("doc:name") && !name.equals("doc:id")) {
        attrPairs.add(name + "=" + attr.getNodeValue());
      }
    }

    Collections.sort(attrPairs);
    for (String pair : attrPairs) {
      signature.append(pair).append(";");
    }

    return signature.toString();
  }
}
