package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.SecurityPatterns;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Dangerous Java class usage.
 */
@Rule(key = "MS016")
public class JavaClassSecurityCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS016";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for dangerous class usage
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      checkForDangerousClasses(context, inputFile, javaBlock.code);
    }

    // Check Java invocations
    for (MuleSoftFileParser.JavaInvocation invocation : parsedFile.javaInvocations) {
      if (invocation.className != null && SecurityPatterns.isDangerousClass(invocation.className)) {
        String reason = SecurityPatterns.getDangerousClassReason(invocation.className);
        reportIssue(context, inputFile, "Potentially dangerous class '" + invocation.className + "' used. " + reason);
      }
    }
  }

  private void checkForDangerousClasses(SensorContext context, InputFile inputFile, String code) {
    if (code == null) {
      return;
    }

    // Check for dangerous classes in code
    if ((code.contains("Runtime") && code.contains("exec")) ||
        (code.contains("ProcessBuilder") && code.contains("new"))) {
      reportIssue(context, inputFile, "Runtime.exec() or ProcessBuilder detected. Can execute arbitrary system commands.");
    }

    if (code.contains("ScriptEngine") || code.contains("GroovyShell")) {
      reportIssue(context, inputFile, "Script engine detected. Can execute arbitrary code.");
    }

    if (code.contains("Class.forName") || code.contains("loadClass")) {
      reportIssue(context, inputFile, "Dynamic class loading detected. Review for security implications.");
    }
  }
}
