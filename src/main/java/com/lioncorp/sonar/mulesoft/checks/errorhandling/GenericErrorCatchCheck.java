package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Generic error catch.
 */
@Rule(key = "MS087")
public class GenericErrorCatchCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS087";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check all error handlers for overly generic error type catching
    for (MuleSoftFileParser.ErrorHandler handler : parsedFile.errorHandlers) {
      if (handler.element != null && catchesGenericError(handler.element)) {
        reportIssue(context, inputFile,
            "Error handler catches overly generic error type 'ANY'. " +
            "This can hide specific errors and make debugging difficult. " +
            "Catch specific error types (e.g., HTTP:CONNECTIVITY, VALIDATION:INVALID_DATA) " +
            "and use 'ANY' only as a last resort for truly unexpected errors.");
      }
    }
  }

  private boolean catchesGenericError(org.w3c.dom.Element errorHandler) {
    String type = errorHandler.getAttribute("type");
    String when = errorHandler.getAttribute("when");

    // Check if the error handler catches ANY without additional filtering
    if ("ANY".equalsIgnoreCase(type)) {
      // If there's a 'when' condition, it's more specific, so it's acceptable
      return when == null || when.trim().isEmpty();
    }

    // Also check for error types that are too broad
    if (type != null) {
      String upperType = type.toUpperCase();
      // These are overly generic patterns
      if (upperType.equals("MULE:ANY") ||
          upperType.equals("*") ||
          upperType.equals("UNKNOWN")) {
        return true;
      }
    }

    return false;
  }
}
