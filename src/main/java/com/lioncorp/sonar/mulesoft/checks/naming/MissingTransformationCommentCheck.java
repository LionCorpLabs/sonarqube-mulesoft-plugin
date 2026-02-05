package com.lioncorp.sonar.mulesoft.checks.naming;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing transformation comment.
 */
@Rule(key = "MS071")
public class MissingTransformationCommentCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS071";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    String content = parsedFile.rawContent;

    // Check for DataWeave transformations (ee:transform, dw:set-payload, dw:set-variable)
    if (hasDataWeaveTransformation(content) && !hasTransformationComment(content)) {
      reportIssue(context, inputFile,
          "DataWeave transformation found without explanatory comment. Add comments to explain complex transformations for maintainability.");
    }
  }

  private boolean hasDataWeaveTransformation(String content) {
    return content.contains("<ee:transform") ||
           content.contains("<dw:set-payload") ||
           content.contains("<dw:set-variable") ||
           content.contains("application/dw") ||
           content.contains("%dw 2.0");
  }

  private boolean hasTransformationComment(String content) {
    // Check for XML comments near transformations
    // Look for <!-- comments --> or doc:description near transform elements
    return (content.contains("<!--") && content.contains("transform")) ||
           (content.contains("doc:description") && content.contains("transform"));
  }
}
