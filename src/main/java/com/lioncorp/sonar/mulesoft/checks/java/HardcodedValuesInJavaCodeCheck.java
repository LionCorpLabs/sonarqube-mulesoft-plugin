package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detect hardcoded values in Java code that should be externalized.
 */
@Rule(key = "MS109")
public class HardcodedValuesInJavaCodeCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS109";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  // Pattern for IP addresses
  private static final Pattern IP_PATTERN = Pattern.compile("\"\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\"");

  // Pattern for URLs
  private static final Pattern URL_PATTERN = Pattern.compile("\"https?://[^\"]+\"");

  // Pattern for file paths
  private static final Pattern FILE_PATH_PATTERN = Pattern.compile("\"[/\\\\][^\"]+\"");

  // Pattern for database connection strings
  private static final Pattern DB_PATTERN = Pattern.compile("\"jdbc:[^\"]+\"");

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for hardcoded values
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForHardcodedValues(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForHardcodedValues(SensorContext context, InputFile inputFile,
                                       MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check for hardcoded IP addresses
    Matcher ipMatcher = IP_PATTERN.matcher(code);
    if (ipMatcher.find()) {
      String ip = ipMatcher.group();
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " contains hardcoded IP address: " + ip + ". " +
          "IP addresses should be externalized to properties files for environment-specific configuration.");
    }

    // Check for hardcoded URLs
    Matcher urlMatcher = URL_PATTERN.matcher(code);
    if (urlMatcher.find()) {
      String url = urlMatcher.group();
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " contains hardcoded URL: " + url + ". " +
          "URLs should be externalized to properties files to support different environments.");
    }

    // Check for hardcoded file paths
    Matcher filePathMatcher = FILE_PATH_PATTERN.matcher(code);
    if (filePathMatcher.find()) {
      String path = filePathMatcher.group();
      if (!isConstantDeclaration(code, path)) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " contains hardcoded file path: " + path + ". " +
            "File paths should be externalized or use environment-independent relative paths.");
      }
    }

    // Check for hardcoded database connection strings
    Matcher dbMatcher = DB_PATTERN.matcher(code);
    if (dbMatcher.find()) {
      String dbUrl = dbMatcher.group();
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " contains hardcoded database connection: " + dbUrl + ". " +
          "Database connections should be configured externally for security and flexibility.");
    }

    // Check for hardcoded credentials patterns
    if (hasHardcodedCredentials(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " appears to contain hardcoded credentials. " +
          "Never hardcode passwords, API keys, or secrets. Use secure property files or vaults.");
    }

    // Check for hardcoded port numbers
    if (hasHardcodedPorts(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " contains hardcoded port numbers. " +
          "Port numbers should be externalized to support different environments and configurations.");
    }

    // Check for magic numbers that should be constants
    if (hasMagicNumbers(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " contains magic numbers. " +
          "Extract numeric literals to named constants for better readability and maintainability. " +
          "Example: private static final int MAX_RETRIES = 3;");
    }
  }

  private boolean isConstantDeclaration(String code, String value) {
    // Check if the value is part of a constant declaration (static final)
    String[] lines = code.split("\n");
    for (String line : lines) {
      if (line.contains(value) && line.contains("static") && line.contains("final")) {
        return true;
      }
    }
    return false;
  }

  private boolean hasHardcodedCredentials(String code) {
    String lowerCode = code.toLowerCase();
    return (lowerCode.contains("password") || lowerCode.contains("passwd") ||
            lowerCode.contains("pwd") || lowerCode.contains("apikey") ||
            lowerCode.contains("api_key") || lowerCode.contains("secret") ||
            lowerCode.contains("token")) &&
           code.contains("= \"") &&
           !code.contains("System.getProperty") &&
           !code.contains("System.getenv") &&
           !code.contains("properties.get");
  }

  private boolean hasHardcodedPorts(String code) {
    // Look for common port number assignments
    Pattern portPattern = Pattern.compile("(port|Port|PORT)\\s*=\\s*(\\d{2,5})");
    Matcher matcher = portPattern.matcher(code);
    if (matcher.find()) {
      String portStr = matcher.group(2);
      int port = Integer.parseInt(portStr);
      // Common well-known ports
      return port > 0 && port < 65536 && !isConstantDeclaration(code, portStr);
    }
    return false;
  }

  private boolean hasMagicNumbers(String code) {
    // Look for numeric literals that aren't part of constant declarations
    // Exclude 0, 1, -1 as they're commonly used
    Pattern magicNumberPattern = Pattern.compile("\\b(?<!final\\s)(?<!static\\s)([2-9]\\d+|\\d{3,})\\b");
    Matcher matcher = magicNumberPattern.matcher(code);

    if (matcher.find()) {
      String number = matcher.group();
      // Check if it's in a constant declaration or array index
      String[] lines = code.split("\n");
      for (String line : lines) {
        if (line.contains(number)) {
          if (line.contains("static final") || line.contains("final static")) {
            continue;
          }
          if (line.contains("[" + number + "]") || line.contains("[" + number + " ")) {
            continue;
          }
          return true;
        }
      }
    }
    return false;
  }
}
