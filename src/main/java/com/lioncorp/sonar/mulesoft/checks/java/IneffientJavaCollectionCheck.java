package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Detect inefficient collection usage patterns in Java code.
 */
@Rule(key = "MS103")
public class IneffientJavaCollectionCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS103";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for inefficient collection usage
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForInefficientCollections(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForInefficientCollections(SensorContext context, InputFile inputFile,
                                              MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check for ArrayList with frequent insertions at beginning
    if (usesArrayListWithFrequentInsertions(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses ArrayList with frequent insertions at index 0. " +
          "ArrayList.add(0, item) is O(n) operation. Use LinkedList for frequent insertions/deletions, " +
          "or add to end and reverse once if order matters.");
    }

    // Check for LinkedList with index-based access
    if (usesLinkedListWithIndexAccess(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses LinkedList with index-based access. " +
          "LinkedList.get(i) is O(n) operation. Use ArrayList for random access, " +
          "or use iterator for sequential access.");
    }

    // Check for Vector instead of ArrayList (synchronized overhead)
    if (code.contains("Vector<") || code.contains("new Vector(")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses Vector which has synchronized overhead. " +
          "Use ArrayList for single-threaded code, or Collections.synchronizedList() if synchronization is needed.");
    }

    // Check for Hashtable instead of HashMap
    if (code.contains("Hashtable<") || code.contains("new Hashtable(")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses Hashtable which has synchronized overhead. " +
          "Use HashMap for single-threaded code, or ConcurrentHashMap for concurrent access.");
    }

    // Check for inefficient iteration with size() in loop condition
    if (hasInefficientLoopCondition(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " calls size() or length() in loop condition. " +
          "Cache the size in a variable before the loop to avoid repeated method calls. " +
          "Example: int size = list.size(); for (int i = 0; i < size; i++)");
    }

    // Check for contains() in loop (O(n^2))
    if (hasContainsInLoop(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " calls contains() inside a loop (O(n²) complexity). " +
          "Use Set instead of List for membership checks, or convert List to Set before the loop.");
    }

    // Check for creating collections without initial capacity
    if (createsCollectionWithoutCapacity(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " creates collections without initial capacity. " +
          "When size is known or predictable, specify initial capacity to avoid resizing overhead. " +
          "Example: new ArrayList<>(expectedSize)");
    }

    // Check for stream operations on small collections where iteration would be better
    if (usesStreamForSimpleOperation(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses Stream API for simple operations. " +
          "For small collections or simple operations, traditional loops may be more efficient due to Stream overhead.");
    }

    // Check for repeated boxing/unboxing in loops
    if (hasBoxingInLoop(code)) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " has boxing/unboxing operations in loop. " +
          "Use primitive arrays or specialized collections (IntArrayList, LongArrayList) to avoid boxing overhead.");
    }
  }

  private boolean usesArrayListWithFrequentInsertions(String code) {
    return (code.contains("ArrayList<") || code.contains("new ArrayList(")) &&
           (code.contains(".add(0,") || code.contains(".add(0 ,") ||
            code.contains(".remove(0)") && code.contains("for "));
  }

  private boolean usesLinkedListWithIndexAccess(String code) {
    return (code.contains("LinkedList<") || code.contains("new LinkedList(")) &&
           (code.contains(".get(") && code.contains("for (int"));
  }

  private boolean hasInefficientLoopCondition(String code) {
    return code.matches("(?s).*for\\s*\\([^;]+;[^;]*\\.size\\(\\)[^;]*;.*") ||
           code.matches("(?s).*for\\s*\\([^;]+;[^;]*\\.length\\(\\)[^;]*;.*") ||
           code.matches("(?s).*while\\s*\\([^)]*\\.size\\(\\)[^)]*\\).*");
  }

  private boolean hasContainsInLoop(String code) {
    return (code.contains("for ") || code.contains("while ")) &&
           code.contains(".contains(") &&
           (code.contains("List<") || code.contains("ArrayList<") || code.contains("LinkedList<"));
  }

  private boolean createsCollectionWithoutCapacity(String code) {
    // Check if collections are created without capacity and there are loops suggesting size is known
    boolean createsCollectionWithoutCapacity =
        code.contains("new ArrayList<>()") || code.contains("new ArrayList()") ||
        code.contains("new HashMap<>()") || code.contains("new HashMap()") ||
        code.contains("new HashSet<>()") || code.contains("new HashSet()");

    boolean hasLoopsWithKnownSize = code.contains("for (") &&
                                   (code.contains(".size()") || code.contains(".length"));

    return createsCollectionWithoutCapacity && hasLoopsWithKnownSize;
  }

  private boolean usesStreamForSimpleOperation(String code) {
    // Check if Stream is used for very simple operations
    return code.contains(".stream()") &&
           (code.contains(".findFirst()") || code.contains(".count()")) &&
           !code.contains(".filter(") && !code.contains(".map(");
  }

  private boolean hasBoxingInLoop(String code) {
    // Check for Integer/Long/Double usage in loops with primitives
    return (code.contains("for ") || code.contains("while ")) &&
           ((code.contains("Integer ") && code.contains("int ")) ||
            (code.contains("Long ") && code.contains("long ")) ||
            (code.contains("Double ") && code.contains("double ")));
  }
}
