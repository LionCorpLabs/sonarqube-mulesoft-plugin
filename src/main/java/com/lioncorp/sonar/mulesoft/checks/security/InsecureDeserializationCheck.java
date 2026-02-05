package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Insecure deserialization detected.
 */
@Rule(key = "MS007")
public class InsecureDeserializationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS007";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for insecure deserialization
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        String code = javaBlock.code;
        if ((StringUtils.containsAllIgnoreCase(code, "ObjectInputStream", "readObject")) ||
            (StringUtils.containsAllIgnoreCase(code, "XMLDecoder", "readObject")) ||
            (StringUtils.containsAllIgnoreCase(code, "Serializable", "readObject("))) {
          reportIssue(context, inputFile, "Insecure deserialization detected. Avoid deserializing untrusted data.");
        }
      }
    }
  }
}
