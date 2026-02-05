package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Insecure random number generation.
 */
@Rule(key = "MS010")
public class InsecureRandomnessCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS010";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for insecure random usage
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null && usesInsecureRandom(javaBlock.code)) {
        reportIssue(context, inputFile,
            "Insecure random number generation detected. Use java.security.SecureRandom instead of Math.random() or java.util.Random for security-sensitive operations.");
      }
    }

    // Check raw content for DataWeave or MEL expressions using random functions
    if (parsedFile.rawContent != null) {
      String content = parsedFile.rawContent;
      if (StringUtils.containsAnyIgnoreCase(content, "randomInt(", "random()")) {
        // Check if it's in a security context
        if (isSecuritySensitiveContext(content)) {
          reportIssue(context, inputFile,
              "Insecure random function usage in DataWeave/MEL. For security-sensitive operations like tokens or keys, use cryptographically secure random functions.");
        }
      }
    }
  }

  private boolean usesInsecureRandom(String code) {
    if (code == null) {
      return false;
    }
    return StringUtils.containsAnyIgnoreCase(code, "Math.random()", "new Random(") &&
           !StringUtils.containsIgnoreCase(code, "SecureRandom");
  }

  private boolean isSecuritySensitiveContext(String content) {
    if (content == null) {
      return false;
    }
    return StringUtils.containsAnyIgnoreCase(content, "token", "password", "key", "secret", "session", "auth");
  }
}
