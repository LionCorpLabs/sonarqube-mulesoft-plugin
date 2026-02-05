package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.regex.Pattern;

/**
 * Detect missing null checks in Java code that could lead to NullPointerException.
 */
@Rule(key = "MS098")
public class MissingNullCheckCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS098";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  // Patterns that suggest potential NPE
  private static final String[] NPE_PRONE_PATTERNS = {
      ".get(", ".toString(", ".length(", ".size(", ".isEmpty(",
      ".equals(", ".contains(", ".charAt(", ".substring(",
      ".split(", ".trim(", ".toLowerCase(", ".toUpperCase(",
      ".getClass(", ".hashCode(", ".compareTo("
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for missing null checks
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForMissingNullChecks(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForMissingNullChecks(SensorContext context, InputFile inputFile,
                                         MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check for method parameters without null checks
    if (hasMethodParameters(code) && !hasNullChecks(code)) {
      // Check if code has NPE-prone operations
      for (String pattern : NPE_PRONE_PATTERNS) {
        if (code.contains(pattern)) {
          reportIssue(context, inputFile,
              JAVA_CODE_PREFIX + javaBlock.type + " has method calls without null checks. " +
              "Method parameters and return values should be checked for null to prevent NullPointerException. " +
              "Use if (obj != null) or Objects.requireNonNull().");
          break;
        }
      }
    }

    // Check for dereferencing potentially null collections
    if (hasPotentiallyNullCollection(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " may dereference null collections. " +
          "Check collection variables for null before calling methods like size(), isEmpty(), or iterating.");
    }

    // Check for array access without null check
    if (hasArrayAccess(code) && !hasNullChecks(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " accesses arrays without null checks. " +
          "Array variables should be checked for null before indexing to prevent NullPointerException.");
    }

    // Check for chained method calls (potential NPE if intermediate result is null)
    if (hasChainedMethodCalls(code) && !hasNullChecks(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " has chained method calls without null checks. " +
          "Chained calls (e.g., obj.getX().getY()) can throw NPE if intermediate results are null. " +
          "Use Optional or check each step for null.");
    }

    // Check for map.get() without containsKey()
    if (code.contains(".get(") && code.matches("(?s).*\\bMap\\b.*") && !code.contains(".containsKey(")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " calls Map.get() without checking containsKey(). " +
          "Map.get() returns null if key doesn't exist. " +
          "Use containsKey() or getOrDefault() to handle missing keys safely.");
    }
  }

  private boolean hasMethodParameters(String code) {
    // Look for method definitions with parameters
    Pattern methodPattern = Pattern.compile("\\s+(public|private|protected)\\s+\\w+\\s+\\w+\\s*\\([^)]+\\)");
    return methodPattern.matcher(code).find();
  }

  private boolean hasNullChecks(String code) {
    return code.contains("!= null") ||
           code.contains("== null") ||
           code.contains("Objects.requireNonNull") ||
           code.contains("Objects.nonNull") ||
           code.contains("Optional.") ||
           code.contains("@NonNull") ||
           code.contains("@Nullable") ||
           code.contains("@NotNull");
  }

  private boolean hasPotentiallyNullCollection(String code) {
    // Check if collection variables are used without null checks
    return (code.contains("List ") || code.contains("Set ") ||
            code.contains("Collection ") || code.contains("Map ")) &&
           (code.contains(".size()") || code.contains(".isEmpty()") ||
            code.contains(".iterator()") || code.contains("for (") ||
            code.contains(".stream()"));
  }

  private boolean hasArrayAccess(String code) {
    // Look for array indexing: variable[index]
    Pattern arrayAccessPattern = Pattern.compile("\\w+\\[\\w+\\]");
    return arrayAccessPattern.matcher(code).find() || code.contains("].") || code.contains("][");
  }

  private boolean hasChainedMethodCalls(String code) {
    // Look for chained method calls: obj.method1().method2()
    Pattern chainPattern = Pattern.compile("\\w+\\([^)]*\\)\\.\\w+\\(");
    return chainPattern.matcher(code).find();
  }
}
