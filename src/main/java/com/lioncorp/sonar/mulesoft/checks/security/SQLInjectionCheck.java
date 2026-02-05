package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.SecurityPatterns;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;

/**
 * Potential SQL injection vulnerability.
 */
@Rule(key = "MS003")
public class SQLInjectionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS003";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for SQL injection risks
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (SecurityPatterns.hasSQLInjectionRisk(javaBlock.code)) {
        reportIssue(context, inputFile, "SQL query uses string concatenation. Use parameterized queries to prevent SQL injection.", 1);
      }
    }

    // Check database connectors for unsafe query patterns
    for (MuleSoftFileParser.DatabaseConnector dbConnector : parsedFile.databaseConnectors) {
      checkDatabaseElement(context, inputFile, dbConnector.element, dbConnector.lineNumber);
    }
  }

  private void checkDatabaseElement(SensorContext context, InputFile inputFile, Element element, int lineNumber) {
    if (element == null) {
      return;
    }

    String query = element.getAttribute("sql");
    if (query == null || query.isEmpty()) {
      query = element.getTextContent();
    }

    if (query != null && !query.isEmpty() && hasUnsafeQueryPattern(query)) {
      reportIssue(context, inputFile, "Database query uses direct variable interpolation. Use parameterized queries instead.", lineNumber);
    }
  }

  private boolean hasUnsafeQueryPattern(String query) {
    String lowerQuery = query.toLowerCase();
    boolean hasSQLKeyword = lowerQuery.contains("select") ||
        lowerQuery.contains("insert") ||
        lowerQuery.contains("update") ||
        lowerQuery.contains("delete");

    if (!hasSQLKeyword) {
      return false;
    }

    // Check for variable interpolation without parameters
    boolean hasInterpolation = query.contains("${") || query.contains("#{") || query.contains("#[");
    boolean hasParameters = query.contains(":");

    return hasInterpolation && !hasParameters;
  }
}
