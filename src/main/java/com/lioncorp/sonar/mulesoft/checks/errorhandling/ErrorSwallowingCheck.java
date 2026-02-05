package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Error swallowing detected.
 */
@Rule(key = "MS088")
public class ErrorSwallowingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS088";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.ErrorHandler handler : parsedFile.errorHandlers) {
      // Check if error handler is empty or doesn't handle errors properly
      if (handler.isEmpty || !hasProperErrorHandling(handler)) {
        reportIssue(context, inputFile,
            "Error handler swallows exceptions without logging or propagating. " +
            "Errors should be logged or re-thrown for proper error tracking.",
            handler.lineNumber);
      }
    }

    // Check Java code blocks for exception swallowing
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null && swallowsException(javaBlock.code)) {
        reportIssue(context, inputFile,
            "Java code in " + javaBlock.type + " swallows exceptions. " +
            "Catch blocks should log errors or re-throw them.",
            1);
      }
    }
  }

  private boolean hasProperErrorHandling(MuleSoftFileParser.ErrorHandler handler) {
    if (handler.element == null) {
      return false;
    }

    // Check if handler contains logger components
    boolean hasLogger = containsLogger(handler.element);

    // Check if handler propagates error (on-error-propagate type)
    boolean propagatesError = "on-error-propagate".equals(handler.type);

    // Check if handler has raise-error component
    boolean hasRaiseError = containsRaiseError(handler.element);

    return hasLogger || propagatesError || hasRaiseError;
  }

  private boolean containsLogger(org.w3c.dom.Element element) {
    org.w3c.dom.NodeList loggers = element.getElementsByTagName("logger");
    return loggers.getLength() > 0;
  }

  private boolean containsRaiseError(org.w3c.dom.Element element) {
    org.w3c.dom.NodeList raiseErrors = element.getElementsByTagName("raise-error");
    return raiseErrors.getLength() > 0;
  }

  private boolean swallowsException(String code) {
    if (code == null) {
      return false;
    }
    String lower = code.toLowerCase();
    // Empty catch blocks or catch without logging
    return (lower.contains("catch") && lower.contains("{}")) ||
           (lower.contains("catch") && !lower.contains("log") &&
            !lower.contains("throw") && !lower.contains("raise"));
  }
}
