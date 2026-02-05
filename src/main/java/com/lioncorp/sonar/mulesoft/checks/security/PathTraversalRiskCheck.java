package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.SecurityPatterns;
import com.lioncorp.sonar.mulesoft.utils.StringUtils;
import com.lioncorp.sonar.mulesoft.utils.XmlUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Path traversal vulnerability.
 */
@Rule(key = "MS006")
public class PathTraversalRiskCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS006";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for path traversal patterns
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null && containsPathTraversal(javaBlock.code)) {
        reportIssue(context, inputFile, "Potential path traversal vulnerability. Validate and sanitize file paths.");
      }
    }

    // Check file operations in XML for path traversal patterns
    if (parsedFile.xmlDocument != null) {
      XmlUtils.visitElements(parsedFile.xmlDocument.getDocumentElement(), element -> {
        XmlUtils.checkAttributes(element,
          (attrName, attrValue) ->
            StringUtils.containsAnyIgnoreCase(attrName, "path", "file", "directory") &&
            SecurityPatterns.hasPathTraversalRisk(attrValue),
          attrValue -> reportIssue(context, inputFile, "Path contains traversal sequences ('../'). Validate file paths."));
      });
    }
  }

  private boolean containsPathTraversal(String code) {
    return SecurityPatterns.hasPathTraversalRisk(code) ||
           (code.contains("File(") && code.contains(".."));
  }
}
