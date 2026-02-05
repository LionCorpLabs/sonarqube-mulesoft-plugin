package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Outdated comment.
 */
@Rule(key = "MS067")
public class OutdatedCommentCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS067";
  }

  private static final String[] OUTDATED_INDICATORS = {
      "deprecated", "old version", "legacy", "obsolete", "no longer",
      "temporary fix", "workaround", "2019", "2020", "2021", "2022"
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    String content = parsedFile.rawContent.toLowerCase();

    for (String indicator : OUTDATED_INDICATORS) {
      if (content.contains(indicator)) {
        reportIssue(context, inputFile,
            "File contains potentially outdated comment with '" + indicator + "'. Review and update or remove stale comments.");
        break;
      }
    }
  }
}
