package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Missing batch processing.
 */
@Rule(key = "MS076")
public class MissingBatchProcessingCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS076";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Check for foreach loops with operations that could benefit from batch processing
    org.w3c.dom.NodeList foreachNodes = parsedFile.xmlDocument.getElementsByTagName("foreach");

    for (int i = 0; i < foreachNodes.getLength(); i++) {
      org.w3c.dom.Element foreachElement = (org.w3c.dom.Element) foreachNodes.item(i);

      // Check if the foreach contains operations suitable for batch processing
      if (containsDatabaseOperation(foreachElement)) {
        reportIssue(context, inputFile,
            "Database operations inside foreach loop detected. Consider using batch:job with " +
            "batch:step for better performance and reliability. Batch processing provides " +
            "transaction management, error handling, and parallel processing capabilities.");
      }

      if (containsHTTPRequest(foreachElement)) {
        reportIssue(context, inputFile,
            "HTTP requests inside foreach loop detected. Consider using batch processing or " +
            "scatter-gather to parallelize requests and improve performance.");
      }

      if (containsFileOperation(foreachElement)) {
        reportIssue(context, inputFile,
            "File operations inside foreach loop detected. Consider using batch processing " +
            "with batch:commit to handle large file sets more efficiently.");
      }
    }

    // Check for large dataset processing without batch
    checkForLargeDatasetProcessing(context, inputFile, parsedFile);
  }

  private boolean containsDatabaseOperation(org.w3c.dom.Element element) {
    // Check current element
    if (isDatabaseOperation(element)) {
      return true;
    }

    // Check child elements recursively
    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      org.w3c.dom.Node child = children.item(i);
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        if (containsDatabaseOperation((org.w3c.dom.Element) child)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isDatabaseOperation(org.w3c.dom.Element element) {
    String namespaceURI = element.getNamespaceURI();
    if (namespaceURI != null && (namespaceURI.contains("database") || namespaceURI.contains("/db"))) {
      String localName = element.getLocalName();
      return "select".equals(localName) || "insert".equals(localName) ||
             "update".equals(localName) || "delete".equals(localName) ||
             "bulk-insert".equals(localName);
    }
    return false;
  }

  private boolean containsHTTPRequest(org.w3c.dom.Element element) {
    org.w3c.dom.NodeList httpNodes = element.getElementsByTagNameNS("*", "request");
    for (int i = 0; i < httpNodes.getLength(); i++) {
      org.w3c.dom.Element httpElement = (org.w3c.dom.Element) httpNodes.item(i);
      String namespaceURI = httpElement.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("http")) {
        return true;
      }
    }
    return false;
  }

  private boolean containsFileOperation(org.w3c.dom.Element element) {
    org.w3c.dom.NodeList fileNodes = element.getElementsByTagNameNS("*", "*");
    for (int i = 0; i < fileNodes.getLength(); i++) {
      org.w3c.dom.Element fileElement = (org.w3c.dom.Element) fileNodes.item(i);
      String namespaceURI = fileElement.getNamespaceURI();
      if (namespaceURI != null && namespaceURI.contains("file")) {
        String localName = fileElement.getLocalName();
        if ("write".equals(localName) || "copy".equals(localName) ||
            "move".equals(localName) || "read".equals(localName)) {
          return true;
        }
      }
    }
    return false;
  }

  private void checkForLargeDatasetProcessing(SensorContext context, InputFile inputFile,
                                             MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    // Check for database select without batch processing
    for (MuleSoftFileParser.DatabaseConnector connector : parsedFile.databaseConnectors) {
      if ("select".equals(connector.type)) {
        org.w3c.dom.Element parent = (org.w3c.dom.Element) connector.element.getParentNode();
        if (parent != null && hasFollowingForeach(parent) && !isInBatchJob(connector.element)) {
          reportIssue(context, inputFile,
              "Database select followed by foreach detected. For large datasets, consider using " +
              "batch processing or streaming with watermarking to handle data efficiently.");
        }
      }
    }
  }

  private boolean hasFollowingForeach(org.w3c.dom.Element element) {
    org.w3c.dom.NodeList siblings = element.getChildNodes();
    boolean foundDb = false;

    for (int i = 0; i < siblings.getLength(); i++) {
      org.w3c.dom.Node sibling = siblings.item(i);
      if (sibling.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        org.w3c.dom.Element siblingElement = (org.w3c.dom.Element) sibling;

        if (foundDb && "foreach".equals(siblingElement.getLocalName())) {
          return true;
        }

        if (isDatabaseOperation(siblingElement)) {
          foundDb = true;
        }
      }
    }
    return false;
  }

  private boolean isInBatchJob(org.w3c.dom.Element element) {
    org.w3c.dom.Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
      org.w3c.dom.Element parentElement = (org.w3c.dom.Element) parent;
      String localName = parentElement.getLocalName();
      String namespaceURI = parentElement.getNamespaceURI();

      if (("batch-job".equals(localName) || "batch:job".equals(parentElement.getTagName())) ||
          (namespaceURI != null && namespaceURI.contains("batch"))) {
        return true;
      }
      parent = parent.getParentNode();
    }
    return false;
  }
}
