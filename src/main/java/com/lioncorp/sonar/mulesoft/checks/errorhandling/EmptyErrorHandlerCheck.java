package com.lioncorp.sonar.mulesoft.checks.errorhandling;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Empty error handler.
 */
@Rule(key = "MS086")
public class EmptyErrorHandlerCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS086";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check all error handlers for empty implementations
    for (MuleSoftFileParser.ErrorHandler handler : parsedFile.errorHandlers) {
      if (handler.isEmpty) {
        reportIssue(context, inputFile,
            "Error handler is empty and does not contain any error handling logic. " +
            "Empty error handlers can mask problems and make debugging difficult. " +
            "Add proper error handling such as logging, transformation, or propagation.");
      }
    }
  }
}
