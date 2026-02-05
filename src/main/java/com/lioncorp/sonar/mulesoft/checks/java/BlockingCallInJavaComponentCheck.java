package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Detect blocking I/O calls in Java components.
 * Blocking calls can cause performance issues in MuleSoft's reactive runtime.
 */
@Rule(key = "MS106")
public class BlockingCallInJavaComponentCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS106";
  }
  private static final String JAVA_CODE_PREFIX = "Java code in ";

  private static final String[] BLOCKING_IO_PATTERNS = {
      "FileInputStream", "FileOutputStream", "FileReader", "FileWriter",
      "BufferedReader", "BufferedWriter", "RandomAccessFile",
      "Socket", "ServerSocket", "DatagramSocket",
      "Thread.sleep(", "Thread.join(", "Object.wait(",
      "CountDownLatch.await(", "CyclicBarrier.await(",
      "Semaphore.acquire(", "Lock.lock(",
      ".get()",
      "InputStream.read(", "OutputStream.write(",
      "Scanner", "PrintWriter"
  };

  private static final String[] JDBC_BLOCKING_PATTERNS = {
      "Connection", "Statement", "PreparedStatement", "ResultSet",
      "DriverManager.getConnection", ".executeQuery(", ".executeUpdate(",
      ".execute("
  };

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java code blocks for blocking I/O calls
    for (MuleSoftFileParser.JavaCodeBlock javaBlock : parsedFile.javaCodeBlocks) {
      if (javaBlock.code != null) {
        checkForBlockingCalls(context, inputFile, javaBlock);
      }
    }
  }

  private void checkForBlockingCalls(SensorContext context, InputFile inputFile,
                                     MuleSoftFileParser.JavaCodeBlock javaBlock) {
    String code = javaBlock.code;

    // Check for blocking I/O operations
    for (String pattern : BLOCKING_IO_PATTERNS) {
      if (code.contains(pattern)) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " contains blocking I/O call: " + pattern + ". " +
            "Blocking operations can cause performance issues in MuleSoft's reactive runtime. " +
            "Consider using non-blocking alternatives or async operations.");
        break;
      }
    }

    // Check for JDBC blocking operations
    for (String pattern : JDBC_BLOCKING_PATTERNS) {
      if (code.contains(pattern)) {
        reportIssue(context, inputFile,
            JAVA_CODE_PREFIX + javaBlock.type + " contains blocking JDBC call: " + pattern + ". " +
            "Direct JDBC calls are blocking and can cause performance bottlenecks. " +
            "Use MuleSoft's database connector or implement connection pooling with async patterns.");
        break;
      }
    }

    // Check for synchronized blocks (can cause blocking)
    if (code.contains("synchronized(") || code.contains("synchronized (")) {
      reportIssue(context, inputFile,
          JAVA_CODE_PREFIX + javaBlock.type + " uses synchronized blocks. " +
          "Synchronized blocks can cause thread contention and blocking. " +
          "Consider using non-blocking concurrency primitives or MuleSoft's built-in threading.");
    }
  }
}
