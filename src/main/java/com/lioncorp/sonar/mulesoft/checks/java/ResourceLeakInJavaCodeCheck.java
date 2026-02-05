package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Resource leak in Java code.
 */
@Rule(key = "MS104")
public class ResourceLeakInJavaCodeCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS104";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for resource leaks
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForResourceLeaks(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForResourceLeaks(SensorContext context, InputFile inputFile,
                                      MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check for file streams without try-with-resources or close()
    if (hasResourceLeak(code, "FileInputStream") ||
        hasResourceLeak(code, "FileOutputStream") ||
        hasResourceLeak(code, "FileReader") ||
        hasResourceLeak(code, "FileWriter") ||
        hasResourceLeak(code, "BufferedReader") ||
        hasResourceLeak(code, "BufferedWriter")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " may have resource leak. " +
          "File streams should be used in try-with-resources or explicitly closed.");
    }

    // Check for database connections without proper closing
    if (hasResourceLeak(code, "Connection") ||
        hasResourceLeak(code, "Statement") ||
        hasResourceLeak(code, "PreparedStatement") ||
        hasResourceLeak(code, "ResultSet")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " may have database connection leak. " +
          "Database resources should be used in try-with-resources or explicitly closed.");
    }

    // Check for socket connections without proper closing
    if (hasResourceLeak(code, "Socket") ||
        hasResourceLeak(code, "ServerSocket")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " may have socket connection leak. " +
          "Sockets should be used in try-with-resources or explicitly closed.");
    }
  }

  private boolean hasResourceLeak(String code, String resourceType) {
    if (!code.contains(resourceType)) {
      return false;
    }

    // Check if resource is created with 'new'
    boolean hasNewResource = code.contains("new " + resourceType);
    if (!hasNewResource) {
      return false;
    }

    // Check if code uses try-with-resources (presence of try (...) pattern near resource creation)
    boolean hasTryWithResources = code.matches("(?s).*try\\s*\\([^)]*" + resourceType + "[^)]*\\).*");
    if (hasTryWithResources) {
      return false;
    }

    // Check if there's a .close() call for this resource type
    boolean hasCloseCall = code.contains(".close()");
    if (!hasCloseCall) {
      return true;
    }

    // More sophisticated check: ensure the resource variable is actually closed
    // This is a simplified heuristic - in real analysis we'd parse the AST
    // If we have 'new Resource' but no close in a finally block, it might leak
    return !code.contains("finally");
  }
}
