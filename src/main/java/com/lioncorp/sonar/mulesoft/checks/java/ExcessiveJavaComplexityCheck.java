package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Detect overly complex Java code using cyclomatic complexity heuristics.
 */
@Rule(key = "MS107")
public class ExcessiveJavaComplexityCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS107";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  private static final int MAX_COMPLEXITY = 10;
  private static final int MAX_NESTING_DEPTH = 4;
  private static final int MAX_METHOD_LINES = 50;
  private static final int MAX_PARAMETERS = 5;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for excessive complexity
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForExcessiveComplexity(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForExcessiveComplexity(SensorContext context, InputFile inputFile,
                                           MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Calculate cyclomatic complexity
    int complexity = calculateCyclomaticComplexity(code);
    if (complexity > MAX_COMPLEXITY) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " has cyclomatic complexity of " + complexity +
          " (maximum allowed is " + MAX_COMPLEXITY + "). " +
          "High complexity makes code hard to test and maintain. " +
          "Consider breaking down into smaller methods.");
    }

    // Check nesting depth
    int nestingDepth = calculateMaxNestingDepth(code);
    if (nestingDepth > MAX_NESTING_DEPTH) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " has nesting depth of " + nestingDepth +
          " (maximum allowed is " + MAX_NESTING_DEPTH + "). " +
          "Deep nesting makes code hard to read. " +
          "Use early returns, extract methods, or simplify logic.");
    }

    // Check method length
    int lineCount = code.split("\n").length;
    if (lineCount > MAX_METHOD_LINES) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " has " + lineCount + " lines " +
          "(maximum recommended is " + MAX_METHOD_LINES + "). " +
          "Long methods are hard to understand and maintain. " +
          "Consider extracting smaller, focused methods.");
    }

    // Check parameter count
    int maxParams = countMaxParameters(code);
    if (maxParams > MAX_PARAMETERS) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " has method with " + maxParams + " parameters " +
          "(maximum recommended is " + MAX_PARAMETERS + "). " +
          "Too many parameters suggest the method is doing too much. " +
          "Consider using a parameter object or breaking down the method.");
    }

    // Check for too many return statements
    int returnCount = countOccurrences(code, "return ");
    if (returnCount > 5) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " has " + returnCount + " return statements. " +
          "Multiple return points make code flow hard to follow. " +
          "Consider simplifying the logic or restructuring the method.");
    }
  }

  private int calculateCyclomaticComplexity(String code) {
    int complexity = 1;

    complexity += countOccurrences(code, "if ");
    complexity += countOccurrences(code, "if(");
    complexity += countOccurrences(code, "for ");
    complexity += countOccurrences(code, "for(");
    complexity += countOccurrences(code, "while ");
    complexity += countOccurrences(code, "while(");
    complexity += countOccurrences(code, "case ");
    complexity += countOccurrences(code, "catch ");
    complexity += countOccurrences(code, "catch(");
    complexity += countOccurrences(code, " && ");
    complexity += countOccurrences(code, " || ");
    complexity += countOccurrences(code, " ? ");

    return complexity;
  }

  private int calculateMaxNestingDepth(String code) {
    int currentDepth = 0;
    int maxDepth = 0;

    for (char c : code.toCharArray()) {
      if (c == '{') {
        currentDepth++;
        maxDepth = Math.max(maxDepth, currentDepth);
      } else if (c == '}') {
        currentDepth--;
      }
    }

    return maxDepth;
  }

  private int countMaxParameters(String code) {
    int maxParams = 0;
    String[] lines = code.split("\n");

    for (String line : lines) {
      String trimmed = line.trim();
      if ((trimmed.contains("public ") || trimmed.contains("private ") ||
           trimmed.contains("protected ")) &&
          trimmed.contains("(") && trimmed.contains(")")) {

        int startParen = trimmed.indexOf('(');
        int endParen = trimmed.lastIndexOf(')');
        if (startParen < endParen) {
          String params = trimmed.substring(startParen + 1, endParen).trim();
          if (!params.isEmpty()) {
            int commaCount = countOccurrences(params, ",");
            int paramCount = commaCount + 1;
            maxParams = Math.max(maxParams, paramCount);
          }
        }
      }
    }

    return maxParams;
  }

  private int countOccurrences(String text, String pattern) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(pattern, index)) != -1) {
      count++;
      index += pattern.length();
    }
    return count;
  }
}
