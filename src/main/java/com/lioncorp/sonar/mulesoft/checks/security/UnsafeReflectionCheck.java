package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.SecurityPatterns;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Unsafe reflection usage.
 */
@Rule(key = "MS025")
public class UnsafeReflectionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS025";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for unsafe reflection
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null && hasUnsafeReflection(javaBlock.code)) {
        reportIssue(context, inputFile,
            "Unsafe reflection usage detected. Reflection with user-controlled input can bypass security controls. " +
            "Validate and whitelist class/method names before using reflection.");
      }
    }

    // Check Java invocations for reflection classes
    for (MuleSoftFileParser.JavaInvocation invocation : parsedFile.javaInvocations) {
      if (invocation.className != null && SecurityPatterns.isReflectionClass(invocation.className)) {
        reportIssue(context, inputFile,
            "Reflection class '" + invocation.className + "' used. Ensure reflection is not performed on user-controlled input to prevent security bypass.");
      }
    }
  }

  private boolean hasUnsafeReflection(String code) {
    if (code == null) {
      return false;
    }

    // Check for reflection API usage
    boolean hasReflection = code.contains("Class.forName") ||
                           code.contains("loadClass") ||
                           code.contains("getDeclaredMethod") ||
                           code.contains("getMethod") ||
                           code.contains("invoke") ||
                           code.contains("newInstance");

    if (!hasReflection) {
      return false;
    }

    // Check if reflection is potentially with user input
    return code.contains("payload") ||
           code.contains("message") ||
           code.contains("request") ||
           code.contains("parameter") ||
           code.contains("input") ||
           code.contains("vars.");
  }
}
