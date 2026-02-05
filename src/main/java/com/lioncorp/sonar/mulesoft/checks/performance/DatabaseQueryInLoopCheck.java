package com.lioncorp.sonar.mulesoft.checks.performance;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import com.lioncorp.sonar.mulesoft.utils.DomUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;

/**
 * Database query in loop.
 */
@Rule(key = "MS078")
public class DatabaseQueryInLoopCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS078";
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    if (parsedFile.xmlDocument == null) {
      return;
    }

    // Find all foreach elements
    org.w3c.dom.NodeList foreachNodes = parsedFile.xmlDocument.getElementsByTagName("foreach");

    for (int i = 0; i < foreachNodes.getLength(); i++) {
      org.w3c.dom.Element foreachElement = (org.w3c.dom.Element) foreachNodes.item(i);

      // Check if there are database operations inside foreach
      if (containsDatabaseOperation(foreachElement)) {
        int lineNumber = DomUtils.findLineNumber(foreachElement, parsedFile.rawContent);
        reportIssue(context, inputFile,
            "Database query detected inside foreach loop. This can cause performance issues (N+1 problem). " +
            "Consider using batch operations or refactoring the query.",
            lineNumber);
      }
    }
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
}
