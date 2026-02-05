package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing logger message.
 */
@Rule(key = "MS063")
public class MissingLoggerMessageCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS063";
  }

  private static final String[] MEANINGLESS_MESSAGES = {
      "log", "logging", "here", "test", "debug", "info", "error", "..."
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.LoggerComponent logger : parsedFile.loggers) {
      if (logger.message == null || logger.message.trim().isEmpty()) {
        reportIssue(context, inputFile,
            "Logger component is missing a message. Add a descriptive message to aid debugging.");
      } else if (isMeaninglessMessage(logger.message)) {
        reportIssue(context, inputFile,
            "Logger message '" + logger.message + "' is not meaningful. Use descriptive messages that explain what is being logged and why.");
      }
    }
  }

  private boolean isMeaninglessMessage(String message) {
    String trimmed = message.trim().toLowerCase();
    for (String meaningless : MEANINGLESS_MESSAGES) {
      if (trimmed.equals(meaningless)) {
        return true;
      }
    }
    return trimmed.length() < 5;
  }
}
