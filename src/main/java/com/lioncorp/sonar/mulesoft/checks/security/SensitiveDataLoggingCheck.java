package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.PatternMatcher;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Sensitive data in logs.
 */
@Rule(key = "MS018")
public class SensitiveDataLoggingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS018";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check logger components for logging sensitive data
    for (MuleSoftFileParser.LoggerComponent logger : parsedFile.loggers) {
      checkLoggerForSensitiveData(context, inputFile, logger);
    }
  }

  private void checkLoggerForSensitiveData(SensorContext context, InputFile inputFile, MuleSoftFileParser.LoggerComponent logger) {
    if (logger.message == null || logger.message.isEmpty()) {
      return;
    }

    String message = logger.message;

    // Check if the logger message contains sensitive data patterns
    if (PatternMatcher.containsSensitiveData(message)) {
      reportIssue(context, inputFile,
          "Logger message may contain sensitive data (SSN, credit card, email). " +
          "Ensure sensitive information is not logged or is properly masked.",
          logger.lineNumber);
      return;
    }

    // Check if the logger is referencing sensitive field names
    if (isSensitiveFieldReference(message)) {
      reportIssue(context, inputFile,
          "Logger message references sensitive fields. Ensure sensitive data is not exposed in logs. " +
          "Use masking or filtering to protect sensitive information.",
          logger.lineNumber);
    }
  }

  private boolean isSensitiveFieldReference(String message) {
    if (message == null || message.isEmpty()) {
      return false;
    }

    String[] sensitivePatterns = {
        "password", "passwd", "pwd", "secret", "token", "apikey", "api_key",
        "access_key", "private_key", "auth", "credential", "ssn", "credit_card",
        "card_number", "pin", "account_number", "routing_number"
    };

    String lowerMessage = message.toLowerCase();
    for (String pattern : sensitivePatterns) {
      if (lowerMessage.contains(pattern)) {
        return true;
      }
    }

    return false;
  }
}
