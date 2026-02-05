package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detect unoptimized regex patterns that can cause performance issues.
 */
@Rule(key = "MS110")
public class UnoptimizedJavaRegexCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS110";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  // Pattern to extract regex strings
  private static final Pattern REGEX_PATTERN = Pattern.compile("Pattern\\.compile\\s*\\(\\s*\"([^\"]+)\"");
  private static final Pattern MATCHES_PATTERN = Pattern.compile("\\.matches\\s*\\(\\s*\"([^\"]+)\"");

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for unoptimized regex patterns
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForUnoptimizedRegex(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForUnoptimizedRegex(SensorContext context, InputFile inputFile,
                                        MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check for regex compilation in loops
    if (compilesRegexInLoop(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " compiles regex patterns inside a loop. " +
          "Pattern compilation is expensive. Compile patterns once as static final fields " +
          "and reuse them: private static final Pattern PATTERN = Pattern.compile(\"...\");");
    }

    // Check for String.matches() usage (compiles pattern every time)
    if (code.contains(".matches(\"")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses String.matches() which compiles pattern on every call. " +
          "For repeated use, pre-compile Pattern and use Matcher.matches() instead.");
    }

    // Check for catastrophic backtracking patterns
    Matcher regexMatcher = REGEX_PATTERN.matcher(code);
    while (regexMatcher.find()) {
      String regex = regexMatcher.group(1);
      if (hasCatastrophicBacktracking(regex)) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " contains regex with potential catastrophic backtracking: \"" + regex + "\". " +
            "Patterns with nested quantifiers like (a+)+ can cause exponential time complexity. " +
            "Simplify the pattern or use atomic groups.");
      }
    }

    // Check for inefficient alternation
    regexMatcher = REGEX_PATTERN.matcher(code);
    while (regexMatcher.find()) {
      String regex = regexMatcher.group(1);
      if (hasInefficientAlternation(regex)) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " uses inefficient regex alternation: \"" + regex + "\". " +
            "When alternatives share a common prefix, use (common(a|b)) instead of (commona|commonb). " +
            "Consider using character classes [abc] instead of (a|b|c).");
      }
    }

    // Check for .* without bounds
    regexMatcher = REGEX_PATTERN.matcher(code);
    while (regexMatcher.find()) {
      String regex = regexMatcher.group(1);
      if (hasUnboundedWildcard(regex)) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " uses unbounded .* or .+ in regex: \"" + regex + "\". " +
            "Unbounded wildcards can cause performance issues on large inputs. " +
            "Use more specific patterns or possessive quantifiers (.*+) if backtracking isn't needed.");
      }
    }

    // Check for replaceAll() with simple strings
    if (usesReplaceAllForSimpleString(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses replaceAll() for simple string replacement. " +
          "Use replace() instead of replaceAll() when no regex features are needed. " +
          "replaceAll() compiles a pattern which is slower for literal replacements.");
    }

    // Check for split() with simple delimiters
    if (usesSplitForSimpleDelimiter(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses split() with simple delimiter. " +
          "For single-character delimiters, consider using String.indexOf() in a loop, " +
          "or use Pattern.quote() to avoid regex compilation: split(Pattern.quote(\"|\"))");
    }
  }

  private boolean compilesRegexInLoop(String code) {
    return (code.contains("for ") || code.contains("while ")) &&
           (code.contains("Pattern.compile") || code.contains(".matches(\""));
  }

  private boolean hasCatastrophicBacktracking(String regex) {
    // Check for nested quantifiers like (a+)+ or (a*)*
    return regex.matches(".*\\([^)]*[+*]\\)[+*].*") ||
           regex.matches(".*\\([^)]*[+*]\\)\\{.*") ||
           regex.matches(".*[+*][+*].*");
  }

  private boolean hasInefficientAlternation(String regex) {
    // Check for alternations with common prefixes
    if (!regex.contains("|")) {
      return false;
    }

    String[] alternatives = regex.split("\\|");
    if (alternatives.length > 5) {
      return true;
    }

    // Check if alternatives could be replaced with character class
    if (alternatives.length >= 3) {
      boolean allSingleChar = true;
      for (String alt : alternatives) {
        if (alt.length() != 1) {
          allSingleChar = false;
          break;
        }
      }
      if (allSingleChar) {
        return true;
      }
    }

    return false;
  }

  private boolean hasUnboundedWildcard(String regex) {
    // Check for .* or .+ without clear boundaries
    return (regex.contains(".*") || regex.contains(".+")) &&
           !regex.contains("^") && !regex.contains("$") &&
           regex.length() < 20;
  }

  private boolean usesReplaceAllForSimpleString(String code) {
    // Check if replaceAll is used with literal strings (no regex special chars)
    Pattern replaceAllPattern = Pattern.compile("\\.replaceAll\\s*\\(\\s*\"([^\"]+)\"");
    Matcher matcher = replaceAllPattern.matcher(code);
    while (matcher.find()) {
      String pattern = matcher.group(1);
      // Check if pattern contains regex special characters
      boolean hasRegexChars = pattern.matches(".*[.*+?^${}()\\[\\]\\\\|].*");
      if (!hasRegexChars) {
        return true;
      }
    }
    return false;
  }

  private boolean usesSplitForSimpleDelimiter(String code) {
    // Check if split is used with single character that might be a regex special char
    Pattern splitPattern = Pattern.compile("\\.split\\s*\\(\\s*\"([^\"]{1,2})\"");
    Matcher matcher = splitPattern.matcher(code);
    while (matcher.find()) {
      String delimiter = matcher.group(1);
      // Check if delimiter is a single regex special character
      if (delimiter.length() == 1 && delimiter.matches("[.*+?^${}()\\[\\]\\\\|]")) {
        return true;
      }
    }
    return false;
  }
}
