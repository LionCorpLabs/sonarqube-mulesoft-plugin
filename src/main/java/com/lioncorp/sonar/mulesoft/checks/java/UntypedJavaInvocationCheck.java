package com.lioncorp.sonar.mulesoft.checks.java;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;

/**
 * Detect untyped Java invocations in MuleSoft flows.
 * Java invocations without explicit type information can cause runtime errors.
 */
@Rule(key = "MS097")
public class UntypedJavaInvocationCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS097";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check Java invocations for missing type information
    for (MuleSoftFileParser.JavaInvocation invocation : parsedFile.javaInvocations) {
      checkJavaInvocation(context, inputFile, invocation);
    }
  }

  private void checkJavaInvocation(SensorContext context, InputFile inputFile,
                                   MuleSoftFileParser.JavaInvocation invocation) {
    Element element = invocation.element;

    // Check if class attribute is missing or empty
    if (invocation.className == null || invocation.className.trim().isEmpty()) {
      reportIssue(context, inputFile,
          "Java " + invocation.type + " without class attribute. " +
          "Specify the fully qualified class name to avoid runtime ClassNotFoundException.");
      return;
    }

    // Check if class name is not fully qualified (missing package)
    if (!invocation.className.contains(".")) {
      reportIssue(context, inputFile,
          "Java " + invocation.type + " uses non-qualified class name '" + invocation.className + "'. " +
          "Use fully qualified class names (e.g., com.example.MyClass) to avoid ambiguity and classpath issues.");
      return;
    }

    // For java:invoke, check if method name is specified
    if ("invoke".equals(invocation.type)) {
      if (invocation.methodName == null || invocation.methodName.trim().isEmpty()) {
        reportIssue(context, inputFile,
            "Java invoke without method attribute for class '" + invocation.className + "'. " +
            "Specify the method name to invoke.");
        return;
      }

      // Check if method parameters have type information
      if (element.getElementsByTagName("java:args").getLength() > 0 ||
          element.getElementsByTagName("args").getLength() > 0) {
        // Check if args have types specified
        checkArgumentTypes(context, inputFile, element, invocation);
      }
    }

    // Check for raw types or missing generics in collections
    if (invocation.className.contains("List") || invocation.className.contains("Map") ||
        invocation.className.contains("Set") || invocation.className.contains("Collection")) {
      if (!invocation.className.contains("<")) {
        reportIssue(context, inputFile,
            "Java " + invocation.type + " uses raw collection type '" + invocation.className + "'. " +
            "Use parameterized types (e.g., List<String>) to provide type safety.");
      }
    }
  }

  private void checkArgumentTypes(SensorContext context, InputFile inputFile,
                                  Element element, MuleSoftFileParser.JavaInvocation invocation) {
    // This is a simplified check - in a real implementation, we'd parse the args structure
    String elementString = element.toString();
    if (!elementString.contains("type=") && !elementString.contains("class=")) {
      reportIssue(context, inputFile,
          "Java invoke method '" + invocation.methodName + "' on class '" + invocation.className +
          "' has arguments without explicit type information. " +
          "Specify argument types to avoid method resolution ambiguity.");
    }
  }
}
