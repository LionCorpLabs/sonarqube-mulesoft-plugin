package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * LDAP injection risk.
 */
@Rule(key = "MS029")
public class LDAPInjectionRiskCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS029";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for LDAP injection risks
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        String code = javaBlock.code;
        if (hasLDAPInjectionRisk(code)) {
          reportIssue(context, inputFile, "LDAP query uses string concatenation with user input. Use parameterized queries or proper escaping to prevent LDAP injection.");
        }
      }
    }

    // Check for LDAP connector usage with dynamic input
    String rawContent = parsedFile.rawContent;
    if (rawContent != null && hasUnsafeLDAPPattern(rawContent)) {
      reportIssue(context, inputFile, "LDAP connector uses dynamic input without proper validation. Ensure LDAP queries are properly parameterized.");
    }
  }

  private boolean hasLDAPInjectionRisk(String code) {
    if (code == null) {
      return false;
    }

    String lowerCode = code.toLowerCase();

    // Check for LDAP-related classes and methods
    boolean hasLDAPOperation = lowerCode.contains("initialldapcontext") ||
        lowerCode.contains("initialdircontext") ||
        lowerCode.contains("dircontext") ||
        lowerCode.contains("ldapcontext") ||
        lowerCode.contains(".search(") ||
        lowerCode.contains("ldap://") ||
        lowerCode.contains("ldaps://");

    if (!hasLDAPOperation) {
      return false;
    }

    // Check for string concatenation with user input
    boolean hasStringConcatenation = code.contains("+") ||
        code.contains("concat(") ||
        code.contains("String.format") ||
        lowerCode.contains("\"$") ||
        lowerCode.contains("'$");

    // Check for common user input patterns
    boolean hasUserInput = lowerCode.contains("payload") ||
        lowerCode.contains("request.getparameter") ||
        lowerCode.contains("vars.") ||
        lowerCode.contains("attributes.") ||
        code.contains("${") ||
        code.contains("#{") ||
        code.contains("#[");

    return hasStringConcatenation && hasUserInput;
  }

  private boolean hasUnsafeLDAPPattern(String content) {
    if (content == null) {
      return false;
    }

    String lowerContent = content.toLowerCase();

    // Check for LDAP connector or protocol usage
    boolean hasLDAP = lowerContent.contains("ldap:") ||
        lowerContent.contains("ldaps:") ||
        lowerContent.contains("<ldap:") ||
        lowerContent.contains("ldap-connector");

    if (!hasLDAP) {
      return false;
    }

    // Check for dynamic expressions without validation
    boolean hasDynamicInput = content.contains("${") ||
        content.contains("#{") ||
        content.contains("#[") ||
        lowerContent.contains("payload") ||
        lowerContent.contains("vars.");

    return hasDynamicInput;
  }
}
