package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.SecurityPatterns;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * OS command injection vulnerability.
 */
@Rule(key = "MS005")
public class CommandInjectionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS005";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for command injection risks
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (SecurityPatterns.hasCommandInjectionRisk(javaBlock.code)) {
        reportIssue(context, inputFile, "Potential OS command injection vulnerability. Avoid executing system commands with user input.");
      }
    }
  }
}
