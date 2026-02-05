package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Unhandled custom error.
 */
@Rule(key = "MS096")
public class UnhandledCustomErrorCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS096";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Collect all raised custom error types
    java.util.Set<String> raisedErrorTypes = new java.util.HashSet<>();
    java.util.Set<String> handledErrorTypes = new java.util.HashSet<>();

    // Find all raise-error elements
    if (parsedFile.xmlDocument != null) {
      org.w3c.dom.NodeList raiseErrors = parsedFile.xmlDocument.getElementsByTagName("raise-error");
      for (int i = 0; i < raiseErrors.getLength(); i++) {
        org.w3c.dom.Element raiseError = (org.w3c.dom.Element) raiseErrors.item(i);
        String errorType = raiseError.getAttribute("type");
        if (errorType != null && !errorType.isEmpty() && isCustomErrorType(errorType)) {
          raisedErrorTypes.add(errorType);
        }
      }
    }

    // Find all error handlers and their types
    for (MuleSoftFileParser.ErrorHandler handler : parsedFile.errorHandlers) {
      if (handler.element != null) {
        String handlerType = handler.element.getAttribute("type");
        if (handlerType != null && !handlerType.isEmpty()) {
          handledErrorTypes.add(handlerType);
        }

        // Also check 'when' attribute for additional error types
        String when = handler.element.getAttribute("when");
        if (when != null && !when.isEmpty()) {
          extractErrorTypesFromWhen(when, handledErrorTypes);
        }
      }
    }

    // Check for raised errors that are not handled
    for (String raisedError : raisedErrorTypes) {
      if (!isErrorHandled(raisedError, handledErrorTypes)) {
        reportIssue(context, inputFile,
            "Custom error type '" + raisedError + "' is raised but not explicitly handled. " +
            "Unhandled custom errors may not be properly caught and can result in " +
            "unexpected behavior. Add an error handler for this specific error type or " +
            "ensure it's covered by a broader error handler.");
      }
    }
  }

  private boolean isCustomErrorType(String errorType) {
    if (errorType == null) {
      return false;
    }

    // Custom error types typically have a namespace prefix (e.g., APP:VALIDATION_ERROR)
    // System errors are like MULE:CONNECTIVITY, HTTP:UNAUTHORIZED
    String upper = errorType.toUpperCase();

    // Exclude generic/system error types
    if ("ANY".equals(upper) || "*".equals(upper) || "UNKNOWN".equals(upper)) {
      return false;
    }

    // System error prefixes
    if (upper.startsWith("MULE:") || upper.startsWith("HTTP:") ||
        upper.startsWith("DB:") || upper.startsWith("FILE:") ||
        upper.startsWith("FTP:") || upper.startsWith("SFTP:") ||
        upper.startsWith("JMS:") || upper.startsWith("VM:")) {
      return false;
    }

    // If it has a colon and isn't a system error, it's likely custom
    // Or if it's a custom pattern without colon
    return errorType.contains(":") || upper.matches("^[A-Z_]+$");
  }

  private void extractErrorTypesFromWhen(String when, java.util.Set<String> handledTypes) {
    // Extract error types from 'when' expressions
    // Example: when="#[error.errorType.identifier == 'VALIDATION_ERROR']"
    if (when.contains("error.errorType")) {
      // Simple extraction - look for quoted strings that might be error types
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("['\"]([A-Z_:]+)['\"]");
      java.util.regex.Matcher matcher = pattern.matcher(when);
      while (matcher.find()) {
        handledTypes.add(matcher.group(1));
      }
    }
  }

  private boolean isErrorHandled(String raisedError, java.util.Set<String> handledTypes) {
    // Check exact match
    if (handledTypes.contains(raisedError)) {
      return true;
    }

    // Check for ANY or generic handlers
    if (handledTypes.contains("ANY") || handledTypes.contains("*")) {
      return true;
    }

    // Check for namespace wildcards (e.g., APP:* handles APP:VALIDATION_ERROR)
    for (String handled : handledTypes) {
      if (handled.endsWith(":*") || handled.endsWith("ANY")) {
        String namespace = handled.substring(0, handled.lastIndexOf(':'));
        if (raisedError.startsWith(namespace + ":")) {
          return true;
        }
      }
    }

    return false;
  }
}
