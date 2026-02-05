package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * XML bomb/billion laughs risk.
 */
@Rule(key = "MS030")
public class XMLBombRiskCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS030";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for XML parsers without entity expansion limits
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        String code = javaBlock.code;
        if (hasXMLBombRisk(code)) {
          reportIssue(context, inputFile, "XML parser may be vulnerable to XML bomb attacks. Set entity expansion limits and disable external entity processing.");
        }
      }
    }

    // Check raw content for DOCTYPE declarations with entity definitions
    String rawContent = parsedFile.rawContent;
    if (rawContent != null) {
      if (hasDoctypeWithEntities(rawContent)) {
        reportIssue(context, inputFile, "DOCTYPE declaration with entity definitions detected. This may enable XML bomb or billion laughs attacks.");
      }

      if (hasRecursiveEntityDefinition(rawContent)) {
        reportIssue(context, inputFile, "Recursive entity definition detected. This is a potential XML bomb vulnerability.");
      }
    }
  }

  private boolean hasXMLBombRisk(String code) {
    if (code == null) {
      return false;
    }

    String lowerCode = code.toLowerCase();

    // Check for XML parsing operations
    boolean hasXMLParser = lowerCode.contains("documentbuilder") ||
        lowerCode.contains("saxparser") ||
        lowerCode.contains("xmlreader") ||
        lowerCode.contains("xmlinputfactory") ||
        lowerCode.contains("saxreaderfactory") ||
        lowerCode.contains("xmlstreamreader");

    if (!hasXMLParser) {
      return false;
    }

    // Check if entity expansion limits are set
    boolean hasEntityExpansionLimit = lowerCode.contains("entityexpansionlimit") ||
        lowerCode.contains("entity-expansion-limit") ||
        lowerCode.contains("jdk.xml.entityexpansionlimit");

    // Check if external entities are disabled
    boolean hasExternalEntityProtection = lowerCode.contains("disallow_doctype_decl") ||
        (lowerCode.contains("external-general-entities") && lowerCode.contains("false")) ||
        (lowerCode.contains("external-parameter-entities") && lowerCode.contains("false")) ||
        lowerCode.contains("load-external-dtd");

    // Check if security features are enabled
    boolean hasSecurityFeatures = lowerCode.contains("secure-processing") ||
        lowerCode.contains("setfeature");

    // Vulnerable if parser exists but no protections are in place
    return !hasEntityExpansionLimit && !hasExternalEntityProtection && !hasSecurityFeatures;
  }

  private boolean hasDoctypeWithEntities(String content) {
    if (content == null) {
      return false;
    }

    String upperContent = content.toUpperCase();

    // Check for DOCTYPE declaration
    boolean hasDoctype = upperContent.contains("<!DOCTYPE");

    if (!hasDoctype) {
      return false;
    }

    // Check for entity definitions within DOCTYPE
    return upperContent.contains("<!ENTITY") ||
        upperContent.contains("<!ELEMENT") ||
        content.contains("%") && upperContent.contains("ENTITY");
  }

  private boolean hasRecursiveEntityDefinition(String content) {
    if (content == null) {
      return false;
    }

    String upperContent = content.toUpperCase();

    // Check for entity definitions
    if (!upperContent.contains("<!ENTITY")) {
      return false;
    }

    // Look for recursive patterns where an entity references itself
    // Classic billion laughs pattern: <!ENTITY lol "lol&lol;">
    // or parameter entities: <!ENTITY % a "a%a;">

    // Simple check: if an entity definition contains & followed by the same entity name
    // This catches patterns like: <!ENTITY lol "lol&lol;">
    if (content.matches("(?s).*<!ENTITY\\s+(%\\s+)?([a-zA-Z0-9_]+)\\s+\"[^\"]*&\\2;.*")) {
      return true;
    }

    // Check for nested entity expansion (common in billion laughs)
    // Multiple entities referencing each other
    int entityCount = 0;
    int referenceCount = 0;
    for (String line : content.split("\n")) {
      if (line.toUpperCase().contains("<!ENTITY")) {
        entityCount++;
      }
      // Count entity references (&name;)
      for (int i = 0; i < line.length() - 1; i++) {
        if (line.charAt(i) == '&' && line.indexOf(';', i) > i) {
          referenceCount++;
        }
      }
    }

    // If there are multiple entities and many references, it's suspicious
    return entityCount >= 3 && referenceCount >= entityCount * 2;
  }
}
