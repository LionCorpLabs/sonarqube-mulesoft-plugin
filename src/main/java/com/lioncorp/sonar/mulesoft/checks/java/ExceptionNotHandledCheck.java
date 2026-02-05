package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detect exceptions not properly handled in Java code.
 */
@Rule(key = "MS101")
public class ExceptionNotHandledCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS101";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  // Risky operations that should have exception handling
  private static final String[] RISKY_OPERATIONS = {
      "FileInputStream", "FileOutputStream", "FileReader", "FileWriter",
      "BufferedReader", "BufferedWriter",
      "Socket", "ServerSocket",
      ".parseInt(", ".parseLong(", ".parseDouble(", ".parseFloat(",
      "Class.forName(", ".newInstance(",
      "Connection", "Statement", "PreparedStatement",
      ".invoke(", "Method.invoke(",
      "DocumentBuilder.parse(", "SAXParser.parse(",
      "ObjectInputStream", "ObjectOutputStream"
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for unhandled exceptions
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForUnhandledExceptions(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForUnhandledExceptions(SensorContext context, InputFile inputFile,
                                           MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check if code has any exception handling
    boolean hasTryCatch = code.contains("try") && code.contains("catch");
    boolean hasThrows = code.contains("throws");

    // Check for risky operations without proper exception handling
    for (String riskyOp : RISKY_OPERATIONS) {
      if (code.contains(riskyOp)) {
        if (!hasTryCatch && !hasThrows) {
          reportIssue(context, inputFile,
              JAVA_CODE_PREFIX + javaBlock.type + " contains risky operation '" + riskyOp +
              "' without try-catch or throws declaration. " +
              "Always handle checked exceptions properly to prevent runtime failures.");
          break; // Report once per block
        }
      }
    }

    // Check for empty catch blocks (swallowed exceptions)
    if (hasEmptyCatchBlock(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " contains empty catch block. " +
          "Swallowing exceptions silently hides errors. " +
          "Log the exception or handle it appropriately.");
    }

    // Check for catch(Exception e) without specific handling
    if (catchesTooGeneric(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " catches generic Exception. " +
          "Catching Exception or Throwable is too broad. " +
          "Catch specific exception types to handle different failure scenarios properly.");
    }

    // Check for throwing generic RuntimeException
    if (code.contains("throw new RuntimeException(") || code.contains("throw new Exception(")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " throws generic RuntimeException or Exception. " +
          "Use specific exception types to provide better error context.");
    }
  }

  private boolean hasEmptyCatchBlock(String code) {
    // Pattern to detect empty catch blocks: catch(...) { } or catch(...) { /* comment */ }
    Pattern pattern = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*(?://.*?\\n|/\\*.*?\\*/)?\\s*\\}");
    Matcher matcher = pattern.matcher(code);
    return matcher.find();
  }

  private boolean catchesTooGeneric(String code) {
    // Check for catch(Exception e) or catch(Throwable t)
    return (code.contains("catch") &&
            (code.matches("(?s).*catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\).*") ||
             code.matches("(?s).*catch\\s*\\(\\s*Throwable\\s+\\w+\\s*\\).*"))) &&
           !code.contains("logger") && !code.contains("log.") && !code.contains("LOG.");
  }
}
