package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.regex.Pattern;

/**
 * Detect Java components without proper input validation.
 */
@Rule(key = "MS102")
public class MissingJavaClassValidationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS102";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for missing input validation
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForMissingValidation(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForMissingValidation(SensorContext context, InputFile inputFile,
                                         MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check if method has parameters but no validation
    if (hasMethodParameters(code)) {
      boolean hasValidation = hasInputValidation(code);

      if (!hasValidation) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " accepts parameters without input validation. " +
            "Always validate user input to prevent security vulnerabilities and data corruption. " +
            "Check for null, empty strings, valid ranges, and expected formats.");
      }
    }

    // Check for direct payload/message access without validation
    if (accessesPayloadWithoutValidation(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " accesses MuleSoft payload without validation. " +
          "Validate payload structure, type, and content before processing to prevent runtime errors.");
    }

    // Check for external data usage without sanitization
    if (usesExternalDataWithoutSanitization(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses external data without sanitization. " +
          "Sanitize inputs from HTTP requests, file uploads, or external systems to prevent injection attacks.");
    }

    // Check for direct String concatenation with user input (potential injection)
    if (hasStringConcatenationWithInput(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " concatenates user input directly into strings. " +
          "This can lead to injection vulnerabilities. Use parameterized queries or proper escaping.");
    }
  }

  private boolean hasMethodParameters(String code) {
    // Look for method definitions with parameters
    Pattern methodPattern = Pattern.compile("\\s+(public|private|protected)\\s+\\w+\\s+\\w+\\s*\\([^)]+\\)");
    return methodPattern.matcher(code).find();
  }

  private boolean hasInputValidation(String code) {
    // Look for common validation patterns
    return code.contains("if (") ||
           code.contains("isEmpty()") ||
           code.contains("isBlank()") ||
           code.contains("== null") ||
           code.contains("!= null") ||
           code.contains("Objects.requireNonNull") ||
           code.contains("Assert.") ||
           code.contains("Validate.") ||
           code.contains("Preconditions.") ||
           code.contains(".matches(") ||
           code.contains(".length()") && code.contains("<") ||
           code.contains(".size()") && code.contains(">") ||
           code.contains("Pattern.compile") ||
           code.contains("throw new IllegalArgumentException") ||
           code.contains("@Valid") ||
           code.contains("@NotNull") ||
           code.contains("@NotEmpty");
  }

  private boolean accessesPayloadWithoutValidation(String code) {
    // Check if code accesses payload/message but has no validation
    boolean accessesPayload = code.contains("payload") ||
                              code.contains("message.get") ||
                              code.contains("getPayload()") ||
                              code.contains("getMessage()");

    if (!accessesPayload) {
      return false;
    }

    // Check if there's any validation
    return !hasInputValidation(code) && !code.contains("instanceof");
  }

  private boolean usesExternalDataWithoutSanitization(String code) {
    // Check for external data sources
    boolean usesExternalData = code.contains("request.get") ||
                               code.contains("header.") ||
                               code.contains("queryParam") ||
                               code.contains("pathParam") ||
                               code.contains("cookie") ||
                               code.contains("Scanner") ||
                               code.contains("BufferedReader");

    if (!usesExternalData) {
      return false;
    }

    // Check for sanitization
    boolean hasSanitization = code.contains(".trim()") ||
                              code.contains(".strip()") ||
                              code.contains(".replaceAll(") ||
                              code.contains("Sanitizer") ||
                              code.contains("escape") ||
                              code.contains("encode");

    return !hasSanitization && !hasInputValidation(code);
  }

  private boolean hasStringConcatenationWithInput(String code) {
    // Look for string concatenation patterns that might include user input
    // This is a simplified heuristic
    return (code.contains(" + ") || code.contains("String.format") || code.contains("StringBuilder")) &&
           (code.contains("payload") || code.contains("request") ||
            code.contains("parameter") || code.contains("input")) &&
           (code.contains("query") || code.contains("sql") || code.contains("SELECT") ||
            code.contains("INSERT") || code.contains("UPDATE") || code.contains("DELETE"));
  }
}
