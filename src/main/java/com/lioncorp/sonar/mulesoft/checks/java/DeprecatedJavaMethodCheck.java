package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Detect usage of deprecated Java methods and APIs.
 */
@Rule(key = "MS100")
public class DeprecatedJavaMethodCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS100";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  private static final String[][] DEPRECATED_APIS = {
      // Thread methods
      {"Thread.stop(", "Thread.stop() is deprecated and dangerous. Use interrupt() instead."},
      {"Thread.suspend(", "Thread.suspend() is deprecated. Use wait/notify pattern instead."},
      {"Thread.resume(", "Thread.resume() is deprecated. Use notify/notifyAll instead."},
      {"Thread.destroy(", "Thread.destroy() is deprecated and never implemented."},
      {"Thread.countStackFrames(", "Thread.countStackFrames() is deprecated."},

      // Date/Time methods
      {"Date.parse(", "Date.parse() is deprecated. Use DateTimeFormatter or SimpleDateFormat."},
      {"Date.getYear(", "Date.getYear() is deprecated. Use Calendar.get(Calendar.YEAR) or LocalDate."},
      {"Date.setYear(", "Date.setYear() is deprecated. Use Calendar.set() or LocalDate."},
      {"Date.getMonth(", "Date.getMonth() is deprecated. Use Calendar or LocalDate."},
      {"Date.getDay(", "Date.getDay() is deprecated. Use Calendar or LocalDate."},
      {"Date.getHours(", "Date.getHours() is deprecated. Use Calendar or LocalDateTime."},
      {"Date.getMinutes(", "Date.getMinutes() is deprecated. Use Calendar or LocalDateTime."},
      {"Date.getSeconds(", "Date.getSeconds() is deprecated. Use Calendar or LocalDateTime."},

      // String methods
      {"String.getBytes(int", "String.getBytes(int...) is deprecated. Use getBytes(Charset)."},

      // Observable
      {"Observable.addObserver(", "Observable is deprecated. Use java.beans.PropertyChangeSupport or reactive streams."},

      // URL constructors
      {"new URL(", "URL constructors are deprecated in newer Java versions. Consider URI.toURL()."},

      // Integer/Long constructors
      {"new Integer(", "Integer constructor is deprecated. Use Integer.valueOf() or autoboxing."},
      {"new Long(", "Long constructor is deprecated. Use Long.valueOf() or autoboxing."},
      {"new Double(", "Double constructor is deprecated. Use Double.valueOf() or autoboxing."},
      {"new Float(", "Float constructor is deprecated. Use Float.valueOf() or autoboxing."},
      {"new Boolean(", "Boolean constructor is deprecated. Use Boolean.valueOf() or autoboxing."},
      {"new Byte(", "Byte constructor is deprecated. Use Byte.valueOf() or autoboxing."},
      {"new Short(", "Short constructor is deprecated. Use Short.valueOf() or autoboxing."},

      // Security Manager
      {"System.setSecurityManager(", "SecurityManager is deprecated for removal. Use alternative security mechanisms."},
      {"System.getSecurityManager(", "SecurityManager is deprecated for removal."}
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for deprecated API usage
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForDeprecatedAPIs(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForDeprecatedAPIs(SensorContext context, InputFile inputFile,
                                      MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check for each deprecated API
    for (String[] deprecatedApi : DEPRECATED_APIS) {
      String pattern = deprecatedApi[0];
      String recommendation = deprecatedApi[1];

      if (code.contains(pattern)) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " uses deprecated API: " + pattern + ". " +
            recommendation);
      }
    }

    // Check for @Deprecated annotation usage
    if (code.contains("@Deprecated") && !code.contains("@SuppressWarnings")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " contains deprecated methods. " +
          "Consider updating to current APIs or documenting why deprecated code is necessary.");
    }
  }
}
