package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Thread safety issue.
 */
@Rule(key = "MS105")
public class ThreadSafetyIssueCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS105";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for thread safety issues
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForThreadSafetyIssues(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForThreadSafetyIssues(SensorContext context, InputFile inputFile,
                                          MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check for static non-final fields (shared mutable state)
    if (hasStaticMutableFields(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses static non-final fields. " +
          "This creates shared mutable state that can cause thread safety issues in MuleSoft's multi-threaded environment. " +
          "Make fields final or use thread-safe alternatives.");
    }

    // Check for non-thread-safe collections in instance variables
    if (hasNonThreadSafeCollections(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses non-thread-safe collections. " +
          "Consider using thread-safe alternatives like ConcurrentHashMap or Collections.synchronizedList().");
    }

    // Check for SimpleDateFormat usage (not thread-safe)
    if (code.contains("SimpleDateFormat") && !code.contains("ThreadLocal")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses SimpleDateFormat which is not thread-safe. " +
          "Use DateTimeFormatter or wrap SimpleDateFormat in ThreadLocal.");
    }
  }

  private boolean hasStaticMutableFields(String code) {
    // Look for static fields that are not final
    String[] lines = code.split("\n");
    for (String line : lines) {
      if (isStaticMutableField(line)) {
        return true;
      }
    }
    return false;
  }

  private boolean isStaticMutableField(String line) {
    String trimmed = line.trim();

    // Skip comments
    if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
      return false;
    }

    // Check for static field declarations without final
    return trimmed.contains("static") && !trimmed.contains("final") &&
           !trimmed.contains("static {") &&
           trimmed.matches(".*\\bstatic\\b.*\\w+\\s+\\w+.*[;=].*");
  }

  private boolean hasNonThreadSafeCollections(String code) {
    String[] nonThreadSafeCollections = {
        "new HashMap<", "new ArrayList<", "new HashSet<",
        "new LinkedList<", "new TreeMap<", "new TreeSet<"
    };

    for (String collection : nonThreadSafeCollections) {
      if (code.contains(collection) && isInstanceFieldCollection(code, collection)) {
        return true;
      }
    }
    return false;
  }

  private boolean isInstanceFieldCollection(String code, String collection) {
    String[] lines = code.split("\n");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.contains(collection) && !line.contains("void ") && !line.contains("return") &&
          !isInsideMethod(lines, i)) {
        return true;
      }
    }
    return false;
  }

  private boolean isInsideMethod(String[] lines, int currentIndex) {
    for (int j = Math.max(0, currentIndex - 5); j < currentIndex; j++) {
      String previousLine = lines[j];
      if ((previousLine.contains("public ") || previousLine.contains("private ") ||
           previousLine.contains("protected ")) &&
          previousLine.contains("(") && previousLine.contains(")")) {
        return true;
      }
    }
    return false;
  }
}
