package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Stack trace exposed.
 */
@Rule(key = "MS093")
public class ExposedStackTraceCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS093";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for stack trace exposure
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null && exposesStackTrace(javaBlock.code)) {
        reportIssue(context, inputFile,
            "Java code in " + javaBlock.type + " exposes stack traces. " +
            "This can reveal sensitive information about the application structure. " +
            "Use generic error messages in responses.",
            1);
      }
    }

    // Check error handlers for stack trace exposure in set-payload or transform message
    for (MuleSoftFileParser.ErrorHandler handler : parsedFile.errorHandlers) {
      if (handler.element != null && exposesStackTraceInErrorHandler(handler.element)) {
        reportIssue(context, inputFile,
            "Error handler exposes stack trace information in the response. " +
            "Avoid including exception details in client responses.",
            handler.lineNumber);
      }
    }

    // Check logger messages that might expose sensitive error details
    for (MuleSoftFileParser.LoggerComponent logger : parsedFile.loggers) {
      if (logger.message != null && containsStackTraceReference(logger.message)) {
        // This is less critical - logging stack traces is okay, just not in responses
        // So we'll only warn if it looks like it's being used in a response context
      }
    }
  }

  private boolean exposesStackTraceInErrorHandler(org.w3c.dom.Element errorHandler) {
    // Check for set-payload that includes exception details
    org.w3c.dom.NodeList setPayloads = errorHandler.getElementsByTagName("set-payload");
    for (int i = 0; i < setPayloads.getLength(); i++) {
      org.w3c.dom.Element payload = (org.w3c.dom.Element) setPayloads.item(i);
      String value = payload.getAttribute("value");
      String textContent = payload.getTextContent();

      if (containsStackTraceReference(value) || containsStackTraceReference(textContent)) {
        return true;
      }
    }

    // Check transform message for exception exposure
    org.w3c.dom.NodeList transforms = errorHandler.getElementsByTagName("ee:transform");
    for (int i = 0; i < transforms.getLength(); i++) {
      org.w3c.dom.Element transform = (org.w3c.dom.Element) transforms.item(i);
      String textContent = transform.getTextContent();
      if (containsStackTraceReference(textContent)) {
        return true;
      }
    }

    return false;
  }

  private boolean exposesStackTrace(String code) {
    if (code == null) {
      return false;
    }
    return code.contains("printStackTrace") ||
           (code.contains("exception") && code.contains("getMessage()")) ||
           code.contains("e.toString()");
  }

  private boolean containsStackTraceReference(String content) {
    if (content == null) {
      return false;
    }

    String lower = content.toLowerCase();
    return lower.contains("printstacktrace") ||
           lower.contains("error.stacktrace") ||
           lower.contains("exception.stacktrace") ||
           (lower.contains("error.") && lower.contains("tostring()")) ||
           (lower.contains("exception.") && lower.contains("tostring()"));
  }
}
