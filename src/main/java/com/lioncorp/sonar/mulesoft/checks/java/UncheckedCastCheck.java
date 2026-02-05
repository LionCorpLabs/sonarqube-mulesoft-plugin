package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detect unchecked type casts that can cause ClassCastException at runtime.
 */
@Rule(key = "MS099")
public class UncheckedCastCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS099";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  // Pattern to detect explicit casts: (Type) expression
  private static final Pattern CAST_PATTERN = Pattern.compile("\\(\\s*([A-Z][\\w.<>\\[\\],\\s]*)\\s*\\)\\s*\\w+");

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for unchecked casts
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForUncheckedCasts(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForUncheckedCasts(SensorContext context, InputFile inputFile,
                                      MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Look for explicit type casts
    Matcher matcher = CAST_PATTERN.matcher(code);
    boolean hasUncheckedCast = false;

    while (matcher.find()) {
      String castType = matcher.group(1);
      hasUncheckedCast = true;

      // Check if the cast is followed by instanceof check
      int castPosition = matcher.start();
      String precedingCode = code.substring(0, Math.max(0, castPosition));

      if (!hasInstanceofCheck(precedingCode, castType)) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " has unchecked cast to type '" + castType + "'. " +
            "Add instanceof check before casting to prevent ClassCastException. " +
            "Example: if (obj instanceof " + castType + ") { " + castType + " typed = (" + castType + ") obj; }");
        break; // Report once per block
      }
    }

    // Check for raw type assignments (implicit casts)
    if (hasRawTypeAssignment(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses raw types causing unchecked conversion. " +
          "Use parameterized types (generics) to avoid ClassCastException at runtime. " +
          "Example: List<String> instead of List.");
    }

    // Check for Object.getClass().cast() without instanceof
    if (code.contains(".cast(") && !code.contains("instanceof")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses Class.cast() without instanceof check. " +
          "Verify type compatibility before casting to prevent ClassCastException.");
    }

    // Check for casting collections without checking element types
    if (hasCastToParameterizedCollection(code) && !hasInstanceofCheck(code, "")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " casts to parameterized collection type. " +
          "Casting to List<String> from List produces unchecked cast warning. " +
          "Verify element types or use type-safe methods.");
    }

    // Check for @SuppressWarnings("unchecked") - indicates developer knows about unchecked cast
    if (code.contains("@SuppressWarnings") && code.contains("\"unchecked\"")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " suppresses unchecked cast warnings. " +
          "While sometimes necessary, suppressing warnings can hide type safety issues. " +
          "Document why the unchecked cast is safe and consider refactoring to avoid it.");
    }
  }

  private boolean hasInstanceofCheck(String code, String type) {
    if (code.contains("instanceof")) {
      if (type.isEmpty()) {
        return true; // Any instanceof check found
      }
      // Check if the specific type is used in instanceof
      String simpleType = type.replaceAll("<.*>", "").trim(); // Remove generics
      return code.contains("instanceof " + simpleType);
    }
    return false;
  }

  private boolean hasRawTypeAssignment(String code) {
    // Check for raw type declarations: List list = ... (without <Type>)
    String[] rawTypes = {"List ", "Map ", "Set ", "Collection ", "ArrayList ", "HashMap ", "HashSet "};

    for (String rawType : rawTypes) {
      // Look for declaration without generics
      Pattern rawPattern = Pattern.compile("\\b" + rawType.trim() + "\\s+\\w+\\s*=");
      if (rawPattern.matcher(code).find() && !code.contains(rawType + "<")) {
        return true;
      }
    }
    return false;
  }

  private boolean hasCastToParameterizedCollection(String code) {
    // Look for casts to generic types: (List<String>) obj
    Pattern genericCastPattern = Pattern.compile("\\(\\s*(List|Map|Set|Collection)[^)]*<[^>]+>[^)]*\\)");
    return genericCastPattern.matcher(code).find();
  }
}
