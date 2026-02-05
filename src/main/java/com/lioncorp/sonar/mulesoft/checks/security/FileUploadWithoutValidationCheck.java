package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * File upload without validation.
 */
@Rule(key = "MS027")
public class FileUploadWithoutValidationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS027";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check flows for file upload without validation
    for (MuleSoftFileParser.MuleSoftFlow flow : parsedFile.flows) {
      if (hasFileUpload(flow) && !hasFileValidation(flow)) {
        reportIssue(context, inputFile,
            "Flow '" + flow.name + "' handles file uploads without validation. " +
            "Validate file type, size, and content to prevent malicious file uploads. " +
            "Check file extensions, MIME types, and scan for malware.");
      }
    }
  }

  private boolean hasFileUpload(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    String flowContent = flow.element.getTextContent();
    if (flowContent == null) {
      return false;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for file upload patterns
    return lowerContent.contains("multipart") ||
           lowerContent.contains("file") && (lowerContent.contains("upload") || lowerContent.contains("attachment")) ||
           lowerContent.contains("content-disposition") ||
           lowerContent.contains("octet-stream");
  }

  private boolean hasFileValidation(MuleSoftFileParser.MuleSoftFlow flow) {
    if (flow.element == null) {
      return false;
    }

    String flowContent = flow.element.getTextContent();
    if (flowContent == null) {
      return false;
    }

    String lowerContent = flowContent.toLowerCase();

    // Check for validation patterns
    return lowerContent.contains("validate") ||
           lowerContent.contains("extension") && lowerContent.contains("allowed") ||
           lowerContent.contains("mime") && lowerContent.contains("type") ||
           lowerContent.contains("content-type") && lowerContent.contains("check") ||
           lowerContent.contains("file") && lowerContent.contains("size") ||
           lowerContent.contains("whitelist");
  }
}
