package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Too many choice branches.
 */
@Rule(key = "MS042")
public class ExcessiveChoiceBranchesCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS042";
  }

  private static final int MAX_BRANCHES = 5;

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.ChoiceRouter router : parsedFile.choiceRouters) {
      if (router.branchCount > MAX_BRANCHES) {
        reportIssue(context, inputFile,
            "Choice router has " + router.branchCount + " branches " +
            "(exceeds threshold of " + MAX_BRANCHES + "). " +
            "Consider using a lookup table, strategy pattern, or splitting into multiple choice routers.");
      }
    }
  }
}
