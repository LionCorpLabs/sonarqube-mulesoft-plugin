package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.constants.CheckConstants;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.SecurityPatterns;
import com.lioncorp.sonar.mulesoft.utils.XmlUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Weak encryption algorithm used.
 */
@Rule(key = "MS008")
public class WeakCryptographyCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS008";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for weak cryptography
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null && SecurityPatterns.isWeakCryptoAlgorithm(javaBlock.code)) {
        reportIssue(context, inputFile, "Weak cryptographic algorithm detected (e.g., DES, MD5, SHA1). Use AES-256 or SHA-256 instead.");
      }
    }

    // Check XML elements for weak crypto algorithms
    if (parsedFile.xmlDocument != null) {
      XmlUtils.visitElements(parsedFile.xmlDocument.getDocumentElement(), element -> {
        XmlUtils.checkAttributes(element, (name, value) -> CheckConstants.isWeakCryptoAlgorithm(value),
          attrValue -> reportIssue(context, inputFile, "Weak cryptographic algorithm '" + attrValue + "' detected. Use strong encryption like AES-256."));
      });
    }
  }
}
