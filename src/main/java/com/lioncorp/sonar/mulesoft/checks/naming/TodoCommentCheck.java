package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * TODO comment found.
 */
@Rule(key = "MS066")
public class TodoCommentCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS066";
  }

  private static final String[] TODO_MARKERS = {"TODO", "FIXME", "HACK", "XXX", "BUG"};

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    String content = parsedFile.rawContent;

    for (String marker : TODO_MARKERS) {
      if (content.contains(marker)) {
        reportIssue(context, inputFile,
            "File contains '" + marker + "' comment markers. These indicate incomplete work. Address them or create proper issue tickets.");
        break;
      }
    }
  }
}
