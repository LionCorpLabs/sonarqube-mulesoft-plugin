package com.lioncorp.sonar.mulesoft.checks;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.Arrays;
import java.util.List;

/**
 * Check for potentially dangerous Java class usage in MuleSoft flows.
 */
@Rule(key = "JavaClassSecurity")
public class JavaClassSecurityCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "JavaClassSecurity";
  }

  private static final List<String> DANGEROUS_CLASSES = Arrays.asList(
      "java.lang.Runtime",
      "java.lang.ProcessBuilder",
      "java.io.FileInputStream",
      "java.io.FileOutputStream",
      "java.net.Socket",
      "java.net.ServerSocket",
      "javax.script.ScriptEngine",
      "groovy.lang.GroovyShell"
  );

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.className != null) {
        checkClassName(context, inputFile, javaBlock.className);
      }
      if (javaBlock.code != null) {
        checkCodeForDangerousClasses(context, inputFile, javaBlock.code);
      }
    }

    // Check Java invocations
    for (MuleSoftFileParser.JavaInvocation invocation : parsedFile.javaInvocations) {
      if (invocation.className != null) {
        checkClassName(context, inputFile, invocation.className);
      }
    }
  }

  private void checkClassName(SensorContext context, InputFile inputFile, String className) {
    for (String dangerousClass : DANGEROUS_CLASSES) {
      if (className.equals(dangerousClass) || className.contains(dangerousClass)) {
        reportIssue(context, inputFile, "Potentially dangerous Java class '" + className + "' referenced in class attribute. Review security implications.");
        break;
      }
    }
  }

  private void checkCodeForDangerousClasses(SensorContext context, InputFile inputFile, String code) {
    for (String dangerousClass : DANGEROUS_CLASSES) {
      if (code.contains(dangerousClass)) {
        reportIssue(context, inputFile, "Potentially dangerous Java class '" + dangerousClass + "' used in embedded Java code. Review security implications.");
      }
    }
  }
}
