package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * XXE injection vulnerability.
 */
@Rule(key = "MS004")
public class XMLExternalEntityInjectionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS004";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for XXE vulnerabilities
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      String code = javaBlock.code;
      if (code != null) {
        String lowerCode = StringUtils.lowerCase(code);
        // Check for XML parsing without proper XXE protection
        if (StringUtils.containsAnyIgnoreCase(lowerCode, "documentbuilder", "saxparser", "xmlreader", "xmlinputfactory") &&
            !StringUtils.containsAnyIgnoreCase(lowerCode, "disallow_doctype_decl", "external-general-entities",
                                                "external-parameter-entities", "setfeature")) {
          reportIssue(context, inputFile, "XML parser may be vulnerable to XXE attacks. Disable external entity processing.");
        }
      }
    }
  }
}
