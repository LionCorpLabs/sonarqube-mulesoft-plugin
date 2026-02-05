package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

import java.util.regex.Pattern;

/**
 * Detect Java classes that should implement Serializable but don't.
 */
@Rule(key = "MS108")
public class MissingSerializableImplementationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS108";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for missing Serializable implementation
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForMissingSerializable(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForMissingSerializable(SensorContext context, InputFile inputFile,
                                           MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check if class is defined in the code
    if (!hasClassDefinition(code)) {
      return;
    }

    // Check if class already implements Serializable
    if (implementsSerializable(code)) {
      return;
    }

    // Check if class needs Serializable
    if (needsSerializable(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " defines a class that should implement Serializable. " +
          "Classes used in MuleSoft flows should be serializable to support clustering, " +
          "object store persistence, and VM transport. Add 'implements Serializable' and define serialVersionUID.");
    }

    // Check for ObjectOutputStream/ObjectInputStream usage without Serializable
    if (usesObjectSerialization(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses object serialization without implementing Serializable. " +
          "Classes that use ObjectOutputStream or ObjectInputStream for persistence must implement Serializable.");
    }

    // Check if class is stored in session/cache
    if (isStoredInSessionOrCache(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " appears to store objects in session or cache. " +
          "Objects stored in distributed sessions or caches should implement Serializable.");
    }

    // Check for missing serialVersionUID
    if (implementsSerializable(code) && !hasSerialVersionUID(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " implements Serializable but lacks serialVersionUID. " +
          "Always declare serialVersionUID to ensure version compatibility during deserialization. " +
          "Example: private static final long serialVersionUID = 1L;");
    }
  }

  private boolean hasClassDefinition(String code) {
    return code.contains("class ") &&
           (code.contains("public class") || code.contains("private class") ||
            code.contains("protected class") || Pattern.compile("\\s+class\\s+").matcher(code).find());
  }

  private boolean implementsSerializable(String code) {
    return code.contains("implements Serializable") ||
           code.contains("implements java.io.Serializable") ||
           code.matches("(?s).*implements\\s+[^{]*Serializable[^{]*\\{.*");
  }

  private boolean needsSerializable(String code) {
    // Classes with instance fields likely need serialization
    boolean hasInstanceFields = code.matches("(?s).*private\\s+\\w+\\s+\\w+\\s*[;=].*") ||
                                code.matches("(?s).*protected\\s+\\w+\\s+\\w+\\s*[;=].*");

    // Classes used as data holders
    boolean hasGettersSetters = code.contains("get") && code.contains("set");

    // Classes that extend other classes (might need serialization)
    boolean extendsClass = code.contains("extends ") && !code.contains("extends Object");

    return hasInstanceFields || hasGettersSetters || extendsClass;
  }

  private boolean usesObjectSerialization(String code) {
    return code.contains("ObjectOutputStream") ||
           code.contains("ObjectInputStream") ||
           code.contains("writeObject(") ||
           code.contains("readObject(");
  }

  private boolean isStoredInSessionOrCache(String code) {
    return code.contains("session.set") ||
           code.contains("session.put") ||
           code.contains("cache.put") ||
           code.contains("Cache.put") ||
           code.contains("objectStore") ||
           code.contains("ObjectStore");
  }

  private boolean hasSerialVersionUID(String code) {
    return code.contains("serialVersionUID") &&
           code.matches("(?s).*private\\s+static\\s+final\\s+long\\s+serialVersionUID.*");
  }
}
