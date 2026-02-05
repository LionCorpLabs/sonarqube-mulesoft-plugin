package com.lioncorp.sonar.mulesoft.checks.structure;

import com.lioncorp.sonar.mulesoft.checks.BaseCheck;
import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Magic number detected.
 */
@Rule(key = "MS045")
public class MagicNumberCheck extends BaseCheck {

  @Override
  protected String getRuleKey() {
    return "MS045";
  }
  private static final Pattern MAGIC_NUMBER_PATTERN = Pattern.compile("(?<![\\w${])\\d{3,}(?![\\w}])");
  private static final Set<String> EXCLUDED_ATTRIBUTES = new HashSet<>();

  static {
    // Attributes that commonly contain legitimate numbers
    EXCLUDED_ATTRIBUTES.add("port");
    EXCLUDED_ATTRIBUTES.add("timeout");
    EXCLUDED_ATTRIBUTES.add("responseTimeout");
    EXCLUDED_ATTRIBUTES.add("connectionTimeout");
    EXCLUDED_ATTRIBUTES.add("queryTimeout");
    EXCLUDED_ATTRIBUTES.add("maxConnections");
    EXCLUDED_ATTRIBUTES.add("poolSize");
  }

  @Override
  public void scanFile(SensorContext context, InputFile inputFile, MuleSoftFileParser.ParsedMuleSoftFile parsedFile) {
    Set<String> reportedMagicNumbers = new HashSet<>();

    if (parsedFile.xmlDocument != null) {
      scanElement(parsedFile.xmlDocument.getDocumentElement(), context, inputFile, reportedMagicNumbers);
    }
  }

  private void scanElement(Element element, SensorContext context, InputFile inputFile, Set<String> reportedMagicNumbers) {
    // Check attributes for magic numbers
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attr = attributes.item(i);
      String attrName = attr.getNodeName();
      String attrValue = attr.getNodeValue();

      // Skip excluded attributes
      if (!EXCLUDED_ATTRIBUTES.contains(attrName) && attrValue != null) {
        // Check for magic numbers in expressions
        if (attrValue.contains("#[") || attrValue.contains("$(")) {
          Matcher matcher = MAGIC_NUMBER_PATTERN.matcher(attrValue);
          while (matcher.find()) {
            String magicNumber = matcher.group();
            if (!isSafeMagicNumber(magicNumber) && !reportedMagicNumbers.contains(magicNumber)) {
              reportedMagicNumbers.add(magicNumber);
              reportIssue(context, inputFile,
                  "Magic number '" + magicNumber + "' detected in " + element.getTagName() +
                  " attribute '" + attrName + "'. Consider extracting it to a named constant or property.");
            }
          }
        }
      }
    }

    // Check text content for magic numbers
    String textContent = element.getTextContent();
    if (textContent != null && !textContent.trim().isEmpty()) {
      if (textContent.contains("#[") || textContent.contains("$(")) {
        Matcher matcher = MAGIC_NUMBER_PATTERN.matcher(textContent);
        while (matcher.find()) {
          String magicNumber = matcher.group();
          if (!isSafeMagicNumber(magicNumber) && !reportedMagicNumbers.contains(magicNumber)) {
            reportedMagicNumbers.add(magicNumber);
            reportIssue(context, inputFile,
                "Magic number '" + magicNumber + "' detected in " + element.getTagName() +
                ". Consider extracting it to a named constant or property.");
          }
        }
      }
    }

    // Recursively check child elements
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element) {
        scanElement((Element) children.item(i), context, inputFile, reportedMagicNumbers);
      }
    }
  }

  private boolean isSafeMagicNumber(String number) {
    // Common safe numbers to ignore
    try {
      int value = Integer.parseInt(number);
      // Ignore common HTTP status codes, small numbers, powers of 10
      return value == 100 || value == 200 || value == 201 || value == 204 ||
             value == 400 || value == 401 || value == 403 || value == 404 || value == 500 ||
             value == 1000 || value == 10000 || value == 100000;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
