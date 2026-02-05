package com.lioncorp.sonar.mulesoft.checks.security;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Session management issue.
 */
@Rule(key = "MS028")
public class SessionManagementIssueCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS028";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for session management issues
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        String code = javaBlock.code;

        // Check for insecure cookie settings
        if (hasInsecureCookieSettings(code)) {
          reportIssue(context, inputFile, "HTTP cookie created without Secure and HttpOnly flags. Set both flags to protect against XSS and man-in-the-middle attacks.");
        }

        // Check for session ID exposure in logs
        if (hasSessionIDInLogs(code)) {
          reportIssue(context, inputFile, "Session ID may be exposed in logs. Avoid logging sensitive session information.");
        }

        // Check for excessive session timeout
        if (hasExcessiveSessionTimeout(code)) {
          reportIssue(context, inputFile, "Session timeout exceeds recommended maximum of 30 minutes. Configure shorter session timeouts to reduce security risks.");
        }
      }
    }

    // Check XML content for session configuration issues
    String rawContent = parsedFile.rawContent;
    if (rawContent != null) {
      if (hasSessionIDInURL(rawContent)) {
        reportIssue(context, inputFile, "Session ID appears in URL or query parameters. Use HTTP-only cookies instead to prevent session fixation attacks.");
      }
    }
  }

  private boolean hasInsecureCookieSettings(String code) {
    if (code == null) {
      return false;
    }

    String lowerCode = code.toLowerCase();

    // Check for cookie creation
    boolean hasCookie = lowerCode.contains("new cookie(") ||
        lowerCode.contains("cookie.") ||
        lowerCode.contains("setcookie") ||
        lowerCode.contains("addcookie");

    if (!hasCookie) {
      return false;
    }

    // Check if secure and httponly flags are missing
    boolean hasSecureFlag = lowerCode.contains("setsecure(true)") ||
        lowerCode.contains("secure: true") ||
        lowerCode.contains("secure=true");

    boolean hasHttpOnlyFlag = lowerCode.contains("sethttponly(true)") ||
        lowerCode.contains("httponly: true") ||
        lowerCode.contains("httponly=true");

    return !hasSecureFlag || !hasHttpOnlyFlag;
  }

  private boolean hasSessionIDInLogs(String code) {
    if (code == null) {
      return false;
    }

    String lowerCode = code.toLowerCase();

    // Check for logging statements
    boolean hasLogging = lowerCode.contains("log.") ||
        lowerCode.contains("logger.") ||
        lowerCode.contains("system.out.print");

    if (!hasLogging) {
      return false;
    }

    // Check for session-related keywords
    return lowerCode.contains("session") ||
        lowerCode.contains("sessionid") ||
        lowerCode.contains("jsessionid") ||
        lowerCode.contains("token");
  }

  private boolean hasExcessiveSessionTimeout(String code) {
    if (code == null) {
      return false;
    }

    String lowerCode = code.toLowerCase();

    // Check for session timeout configuration
    boolean hasSessionTimeout = lowerCode.contains("settimeout") ||
        lowerCode.contains("setmaxinactiveinterval") ||
        lowerCode.contains("session.timeout") ||
        lowerCode.contains("session-timeout");

    if (!hasSessionTimeout) {
      return false;
    }

    // Look for timeout values greater than 30 minutes (1800 seconds)
    // Common patterns: setMaxInactiveInterval(3600), timeout="3600", etc.
    if (code.matches(".*\\b([2-9]\\d{3,}|1[89]\\d{2,}|[2-9]\\d{4,})\\b.*")) {
      return true;
    }

    // Check for timeout in minutes (> 30)
    return code.matches(".*timeout[\"']?\\s*[:=]\\s*[\"']?([3-9]\\d|[1-9]\\d{2,})\\s*(min|minutes).*");
  }

  private boolean hasSessionIDInURL(String content) {
    if (content == null) {
      return false;
    }

    String lowerContent = content.toLowerCase();

    // Check for session ID in URLs or query parameters
    return lowerContent.contains("jsessionid") ||
        lowerContent.contains("sessionid=") ||
        lowerContent.contains("session_id=") ||
        lowerContent.contains("sid=") ||
        (lowerContent.contains("url") && lowerContent.contains("session"));
  }
}
