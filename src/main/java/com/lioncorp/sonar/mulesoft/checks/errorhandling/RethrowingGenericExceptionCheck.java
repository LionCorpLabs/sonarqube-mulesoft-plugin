package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Rethrowing generic exception.
 */
@Rule(key = "MS095")
public class RethrowingGenericExceptionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS095";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check error handlers for rethrowing generic exceptions
    for (MuleSoftFileParser.ErrorHandler handler : parsedFile.errorHandlers) {
      if (handler.element != null && rethrowsGenericException(handler)) {
        reportIssue(context, inputFile,
            "Error handler catches a specific error but re-raises a generic error type. " +
            "This loses important error context and makes debugging difficult. " +
            "Either propagate the original error or raise a specific custom error type.");
      }
    }

    // Check Java code blocks for generic exception rethrowing
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null && containsGenericRethrow(javaBlock.code)) {
        reportIssue(context, inputFile,
            "Java code in " + javaBlock.type + " catches specific exception but rethrows generic Exception. " +
            "Preserve the original exception type or wrap it in a meaningful custom exception.");
      }
    }
  }

  private boolean rethrowsGenericException(MuleSoftFileParser.ErrorHandler handler) {
    String handlerType = handler.element.getAttribute("type");

    // If handler catches a specific error type
    if (handlerType != null && !handlerType.isEmpty() &&
        !"ANY".equalsIgnoreCase(handlerType) &&
        !handlerType.equals("*")) {

      // Check if it raises a generic error
      org.w3c.dom.NodeList raiseErrors = handler.element.getElementsByTagName("raise-error");
      for (int i = 0; i < raiseErrors.getLength(); i++) {
        org.w3c.dom.Element raiseError = (org.w3c.dom.Element) raiseErrors.item(i);
        String raisedType = raiseError.getAttribute("type");

        // Check if raised error is generic
        if (raisedType != null && isGenericErrorType(raisedType)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean isGenericErrorType(String errorType) {
    String upper = errorType.toUpperCase();
    return "ANY".equals(upper) ||
           "MULE:ANY".equals(upper) ||
           "UNKNOWN".equals(upper) ||
           "*".equals(upper) ||
           "ERROR".equals(upper) ||
           "EXCEPTION".equals(upper);
  }

  private boolean containsGenericRethrow(String code) {
    if (code == null) {
      return false;
    }

    String lowerCode = code.toLowerCase();

    // Check for pattern: catch specific exception, throw generic
    // Pattern: catch (SpecificException) { ... throw new Exception(...) }
    boolean hasSpecificCatch = lowerCode.contains("catch") &&
        (lowerCode.contains("ioexception") ||
         lowerCode.contains("sqlexception") ||
         lowerCode.contains("nullpointerexception") ||
         lowerCode.contains("illegalargumentexception"));

    boolean throwsGeneric = lowerCode.contains("throw new exception") ||
        lowerCode.contains("throw new runtimeexception") ||
        lowerCode.contains("throw new throwable");

    return hasSpecificCatch && throwsGeneric;
  }
}
