package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Large payload in memory.
 */
@Rule(key = "MS079")
public class LargePayloadInMemoryCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS079";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check for file operations that might load large files into memory
    checkFileOperations(context, inputFile, parsedFile);

    // Check for DataWeave transformations without streaming
    checkDataWeaveTransformations(context, inputFile, parsedFile);

    // Check for payload logging that could expose large payloads
    checkPayloadLogging(context, inputFile, parsedFile);

    // Check for transformations that materialize the entire payload
    checkPayloadMaterialization(context, inputFile, parsedFile);
  }

  private void checkFileOperations(SensorContext context, InputFile inputFile,
                                   MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check for file:read without streaming
    org.w3c.dom.NodeList fileReadNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "read");

    for (int i = 0; i < fileReadNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) fileReadNodes.item(i);
      String namespaceURI = element.getNamespaceURI();

      if (namespaceURI != null && namespaceURI.contains("file")) {
        String streaming = element.getAttribute("streaming");

        if (!"true".equalsIgnoreCase(streaming)) {
          reportIssue(context, inputFile,
              "File read operation detected without streaming. Large files will be loaded into memory. " +
              "Consider using streaming='true' or auto-paging for large files.");
        }
      }
    }
  }

  private void checkDataWeaveTransformations(SensorContext context, InputFile inputFile,
                                            MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    org.w3c.dom.NodeList transformNodes = parsedFile.xmlDocument.getElementsByTagNameNS("*", "transform");

    for (int i = 0; i < transformNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) transformNodes.item(i);

      // Check if transformation is processing large collections without streaming
      String script = getElementTextContent(element);

      if (script != null) {
        // Check for operations that materialize large datasets
        if (script.contains("sizeOf(payload)") || script.contains("payload as Array")) {
          reportIssue(context, inputFile,
              "DataWeave transformation may load entire payload into memory. " +
              "For large payloads, consider using streaming, pagination, or batch processing.");
        }

        // Check for multiple passes over data
        if (countOccurrences(script, "payload") > 3) {
          reportIssue(context, inputFile,
              "DataWeave script references payload multiple times, which may cause " +
              "repeated deserialization. Consider storing payload in a variable.");
        }
      }
    }
  }

  private void checkPayloadLogging(SensorContext context, InputFile inputFile,
                                   MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    for (MuleSoftFileParser.LoggerComponent logger : parsedFile.loggers) {
      String message = logger.message;

      if (message != null && (message.contains("#[payload]") || message.contains("payload"))) {
        reportIssue(context, inputFile,
            "Logger statement logs entire payload. This can cause memory issues with large payloads. " +
            "Consider logging only specific fields or payload size.");
      }
    }
  }

  private void checkPayloadMaterialization(SensorContext context, InputFile inputFile,
                                          MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check for set-payload with expressions that force materialization
    org.w3c.dom.NodeList setPayloadNodes = parsedFile.xmlDocument.getElementsByTagName("set-payload");

    for (int i = 0; i < setPayloadNodes.getLength(); i++) {
      org.w3c.dom.Element element = (org.w3c.dom.Element) setPayloadNodes.item(i);
      String value = element.getAttribute("value");

      if (value != null && value.contains("payload.^raw")) {
        reportIssue(context, inputFile,
            "Payload materialization detected (payload.^raw). This loads the entire payload " +
            "into memory and can cause performance issues with large payloads.");
      }
    }
  }

  private String getElementTextContent(org.w3c.dom.Element element) {
    StringBuilder content = new StringBuilder();
    org.w3c.dom.NodeList children = element.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE ||
          child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
        content.append(child.getTextContent());
      }
    }

    return content.toString();
  }

  private int countOccurrences(String text, String pattern) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(pattern, index)) != -1) {
      count++;
      index += pattern.length();
    }
    return count;
  }
}
