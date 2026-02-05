package com.lioncorp.sonar.mulesoft.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parser for MuleSoft XML files that extracts both XML structure and embedded Java code.
 */
public class MuleSoftFileParser {

  private static final Logger LOG = Loggers.get(MuleSoftFileParser.class);

  private final JavaParser javaParser;

  public MuleSoftFileParser() {
    this.javaParser = new JavaParser();
  }

  /**
   * Parse a MuleSoft XML file and extract all relevant information.
   *
   * @param xmlContent the XML content as a string
   * @return parsed MuleSoft file structure
   */
  public ParsedMuleSoftFile parse(String xmlContent) {
    ParsedMuleSoftFile result = new ParsedMuleSoftFile();
    result.rawContent = xmlContent;

    try {
      // Parse XML
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

      result.xmlDocument = doc;

      // Extract flows
      result.flows = extractFlows(doc, xmlContent);

      // Extract embedded Java code
      result.javaCodeBlocks = extractJavaCode(doc, xmlContent);

      // Extract Java module invocations
      result.javaInvocations = extractJavaInvocations(doc);

      // Extract error handlers
      result.errorHandlers = extractErrorHandlers(doc, xmlContent);

      // Extract HTTP configurations
      result.httpConfigurations = extractHttpConfigurations(doc, xmlContent);

      // Extract database connectors
      result.databaseConnectors = extractDatabaseConnectors(doc, xmlContent);

      // Extract logger components
      result.loggers = extractLoggers(doc, xmlContent);

      // Extract choice routers
      result.choiceRouters = extractChoiceRouters(doc, xmlContent);

      // Extract flow references
      result.flowReferences = extractFlowReferences(doc, result.flows);

    } catch (Exception e) {
      LOG.error("Error parsing MuleSoft file", e);
    }

    return result;
  }

  private List<MuleSoftFlow> extractFlows(Document doc, String xmlContent) {
    List<MuleSoftFlow> flows = new ArrayList<>();

    NodeList flowNodes = doc.getElementsByTagName("flow");
    for (int i = 0; i < flowNodes.getLength(); i++) {
      Element flowElement = (Element) flowNodes.item(i);
      MuleSoftFlow flow = new MuleSoftFlow();
      flow.name = flowElement.getAttribute("name");
      flow.element = flowElement;
      flow.lineNumber = findLineNumber(flowElement, xmlContent);
      flows.add(flow);
    }

    NodeList subFlowNodes = doc.getElementsByTagName("sub-flow");
    for (int i = 0; i < subFlowNodes.getLength(); i++) {
      Element flowElement = (Element) subFlowNodes.item(i);
      MuleSoftFlow flow = new MuleSoftFlow();
      flow.name = flowElement.getAttribute("name");
      flow.isSubFlow = true;
      flow.element = flowElement;
      flow.lineNumber = findLineNumber(flowElement, xmlContent);
      flows.add(flow);
    }

    return flows;
  }

  private List<JavaCodeBlock> extractJavaCode(Document doc, String xmlContent) {
    List<JavaCodeBlock> javaBlocks = new ArrayList<>();

    // Look for Java code in various places:
    // 1. Custom Java transformers
    NodeList transformers = doc.getElementsByTagName("custom-transformer");
    for (int i = 0; i < transformers.getLength(); i++) {
      Element transformer = (Element) transformers.item(i);
      String className = transformer.getAttribute("class");
      if (className != null && !className.isEmpty()) {
        JavaCodeBlock block = new JavaCodeBlock();
        block.type = "custom-transformer";
        block.className = className;
        javaBlocks.add(block);
      }
    }

    // 2. Java components
    NodeList components = doc.getElementsByTagName("component");
    for (int i = 0; i < components.getLength(); i++) {
      Element component = (Element) components.item(i);
      String className = component.getAttribute("class");
      if (className != null && !className.isEmpty()) {
        JavaCodeBlock block = new JavaCodeBlock();
        block.type = "component";
        block.className = className;
        javaBlocks.add(block);
      }
    }

    // 3. Inline Java code (if any using CDATA)
    extractInlineJavaFromCDATA(doc.getDocumentElement(), javaBlocks, xmlContent);

    return javaBlocks;
  }

  private void extractInlineJavaFromCDATA(Element element, List<JavaCodeBlock> javaBlocks, String xmlContent) {
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
        String cdataContent = child.getTextContent();
        if (looksLikeJavaCode(cdataContent)) {
          JavaCodeBlock block = new JavaCodeBlock();
          block.type = "inline-java";
          block.code = cdataContent;

          // Try to parse it as Java
          try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(cdataContent);
            if (parseResult.isSuccessful()) {
              block.compilationUnit = parseResult.getResult().orElse(null);
            }
          } catch (Exception e) {
            LOG.debug("Could not parse CDATA as Java", e);
          }

          javaBlocks.add(block);
        }
      } else if (child.getNodeType() == Node.ELEMENT_NODE) {
        extractInlineJavaFromCDATA((Element) child, javaBlocks, xmlContent);
      }
    }
  }

  private List<JavaInvocation> extractJavaInvocations(Document doc) {
    List<JavaInvocation> invocations = new ArrayList<>();

    // Look for Java module invocations (java:invoke, java:new, etc.)
    NodeList invokeNodes = doc.getElementsByTagNameNS("*", "invoke");
    for (int i = 0; i < invokeNodes.getLength(); i++) {
      Element invokeElement = (Element) invokeNodes.item(i);
      if (isJavaNamespace(invokeElement)) {
        JavaInvocation invocation = new JavaInvocation();
        invocation.type = "invoke";
        invocation.className = invokeElement.getAttribute("class");
        invocation.methodName = invokeElement.getAttribute("method");
        invocation.element = invokeElement;
        invocations.add(invocation);
      }
    }

    NodeList newNodes = doc.getElementsByTagNameNS("*", "new");
    for (int i = 0; i < newNodes.getLength(); i++) {
      Element newElement = (Element) newNodes.item(i);
      if (isJavaNamespace(newElement)) {
        JavaInvocation invocation = new JavaInvocation();
        invocation.type = "new";
        invocation.className = newElement.getAttribute("class");
        invocation.element = newElement;
        invocations.add(invocation);
      }
    }

    return invocations;
  }

  private boolean isJavaNamespace(Element element) {
    String namespaceURI = element.getNamespaceURI();
    return namespaceURI != null && namespaceURI.contains("java");
  }

  private boolean looksLikeJavaCode(String content) {
    String trimmed = content.trim();
    return trimmed.contains("class ") ||
           trimmed.contains("public ") ||
           trimmed.contains("private ") ||
           trimmed.contains("import ") ||
           (trimmed.contains("{") && trimmed.contains("}"));
  }

  private List<ErrorHandler> extractErrorHandlers(Document doc, String xmlContent) {
    List<ErrorHandler> handlers = new ArrayList<>();

    // Extract error-handler elements
    NodeList errorHandlerNodes = doc.getElementsByTagName("error-handler");
    for (int i = 0; i < errorHandlerNodes.getLength(); i++) {
      Element element = (Element) errorHandlerNodes.item(i);
      ErrorHandler handler = new ErrorHandler();
      handler.type = "error-handler";
      handler.element = element;
      handler.isEmpty = !hasErrorHandlingLogic(element);
      handler.lineNumber = findLineNumber(element, xmlContent);
      handlers.add(handler);
    }

    // Extract on-error-continue and on-error-propagate
    String[] errorTypes = {"on-error-continue", "on-error-propagate"};
    for (String errorType : errorTypes) {
      NodeList nodes = doc.getElementsByTagName(errorType);
      for (int i = 0; i < nodes.getLength(); i++) {
        Element element = (Element) nodes.item(i);
        ErrorHandler handler = new ErrorHandler();
        handler.type = errorType;
        handler.element = element;
        handler.isEmpty = !hasErrorHandlingLogic(element);
        handler.lineNumber = findLineNumber(element, xmlContent);
        handlers.add(handler);
      }
    }

    return handlers;
  }

  private boolean hasErrorHandlingLogic(Element errorHandler) {
    NodeList children = errorHandler.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
        return true;
      }
    }
    return false;
  }

  private List<HttpConfiguration> extractHttpConfigurations(Document doc, String xmlContent) {
    List<HttpConfiguration> configs = new ArrayList<>();
    // Use a set to track elements we've already processed to avoid duplicates
    java.util.Set<Element> processedElements = new java.util.HashSet<>();

    // Extract http:listener elements
    NodeList listenerNodes = doc.getElementsByTagNameNS("*", "listener");
    LOG.debug("Found {} http:listener elements", listenerNodes.getLength());
    for (int i = 0; i < listenerNodes.getLength(); i++) {
      Element element = (Element) listenerNodes.item(i);
      if (isHttpNamespace(element) && !processedElements.contains(element)) {
        processedElements.add(element);
        HttpConfiguration config = new HttpConfiguration();
        config.type = "listener";
        config.element = element;
        config.lineNumber = findLineNumber(element, xmlContent);
        extractHttpDetails(config, element);
        configs.add(config);
        LOG.debug("Added listener config at line {}", config.lineNumber);
      }
    }

    // Extract http:request elements
    NodeList requestNodes = doc.getElementsByTagNameNS("*", "request");
    LOG.debug("Found {} http:request elements", requestNodes.getLength());
    for (int i = 0; i < requestNodes.getLength(); i++) {
      Element element = (Element) requestNodes.item(i);
      if (isHttpNamespace(element) && !processedElements.contains(element)) {
        processedElements.add(element);
        HttpConfiguration config = new HttpConfiguration();
        config.type = "request";
        config.element = element;
        config.lineNumber = findLineNumber(element, xmlContent);
        extractHttpDetails(config, element);
        configs.add(config);
        LOG.debug("Added request config at line {}", config.lineNumber);
      }
    }

    // Extract http:listener-config and http:request-config
    String[] configTypes = {"listener-config", "request-config"};
    for (String configType : configTypes) {
      NodeList nodes = doc.getElementsByTagNameNS("*", configType);
      LOG.debug("Found {} {} elements", nodes.getLength(), configType);
      for (int i = 0; i < nodes.getLength(); i++) {
        Element element = (Element) nodes.item(i);
        if (isHttpNamespace(element) && !processedElements.contains(element)) {
          processedElements.add(element);
          HttpConfiguration config = new HttpConfiguration();
          config.type = configType;
          config.element = element;
          config.lineNumber = findLineNumber(element, xmlContent);
          extractHttpDetails(config, element);
          configs.add(config);
          LOG.debug("Added {} config at line {}", configType, config.lineNumber);
        }
      }
    }

    LOG.debug("Total HTTP configurations extracted: {}", configs.size());
    return configs;
  }

  private boolean isHttpNamespace(Element element) {
    String namespaceURI = element.getNamespaceURI();
    return namespaceURI != null && namespaceURI.contains("http");
  }

  private void extractHttpDetails(HttpConfiguration config, Element element) {
    config.protocol = element.getAttribute("protocol");
    config.host = element.getAttribute("host");
    config.port = element.getAttribute("port");

    // Check nested connection elements (listener-connection, request-connection)
    NodeList connectionNodes = element.getElementsByTagNameNS("*", "listener-connection");
    if (connectionNodes.getLength() == 0) {
      connectionNodes = element.getElementsByTagNameNS("*", "request-connection");
    }
    if (connectionNodes.getLength() > 0) {
      Element connectionElement = (Element) connectionNodes.item(0);
      if (config.protocol == null || config.protocol.isEmpty()) {
        config.protocol = connectionElement.getAttribute("protocol");
      }
      if (config.host == null || config.host.isEmpty()) {
        config.host = connectionElement.getAttribute("host");
      }
      if (config.port == null || config.port.isEmpty()) {
        config.port = connectionElement.getAttribute("port");
      }
    }

    // Check for authentication
    NodeList authNodes = element.getElementsByTagName("authentication");
    config.hasAuthentication = authNodes.getLength() > 0;

    // Check for TLS/SSL
    NodeList tlsNodes = element.getElementsByTagName("tls:context");
    config.hasTLS = tlsNodes.getLength() > 0 ||
                    "HTTPS".equalsIgnoreCase(config.protocol);
  }

  private List<DatabaseConnector> extractDatabaseConnectors(Document doc, String xmlContent) {
    List<DatabaseConnector> connectors = new ArrayList<>();

    // Look for database-related elements (db:config, db:select, etc.)
    String[] dbTypes = {"config", "select", "insert", "update", "delete", "bulk-insert"};
    for (String dbType : dbTypes) {
      NodeList nodes = doc.getElementsByTagNameNS("*", dbType);
      for (int i = 0; i < nodes.getLength(); i++) {
        Element element = (Element) nodes.item(i);
        if (isDbNamespace(element)) {
          DatabaseConnector connector = new DatabaseConnector();
          connector.type = dbType;
          connector.element = element;

          // Check for SSL
          String url = element.getAttribute("url");
          connector.hasSSL = url != null && (url.contains("ssl=true") || url.contains("sslMode="));

          // Check for pooling configuration
          connector.hasPooling = element.hasAttribute("poolingProfile") ||
                                 element.getElementsByTagName("pooling-profile").getLength() > 0;

          connector.lineNumber = findLineNumber(element, xmlContent);
          connectors.add(connector);
        }
      }
    }

    return connectors;
  }

  private boolean isDbNamespace(Element element) {
    String namespaceURI = element.getNamespaceURI();
    return namespaceURI != null && (namespaceURI.contains("database") || namespaceURI.contains("/db"));
  }

  private List<LoggerComponent> extractLoggers(Document doc, String xmlContent) {
    List<LoggerComponent> loggers = new ArrayList<>();

    NodeList loggerNodes = doc.getElementsByTagName("logger");
    for (int i = 0; i < loggerNodes.getLength(); i++) {
      Element element = (Element) loggerNodes.item(i);
      LoggerComponent logger = new LoggerComponent();
      logger.message = element.getAttribute("message");
      logger.level = element.getAttribute("level");
      logger.element = element;
      logger.lineNumber = findLineNumber(element, xmlContent);
      loggers.add(logger);
    }

    return loggers;
  }

  private List<ChoiceRouter> extractChoiceRouters(Document doc, String xmlContent) {
    List<ChoiceRouter> routers = new ArrayList<>();

    NodeList choiceNodes = doc.getElementsByTagName("choice");
    for (int i = 0; i < choiceNodes.getLength(); i++) {
      Element element = (Element) choiceNodes.item(i);
      ChoiceRouter router = new ChoiceRouter();
      router.element = element;

      // Count when/otherwise branches
      int whenCount = element.getElementsByTagName("when").getLength();
      int otherwiseCount = element.getElementsByTagName("otherwise").getLength();
      router.branchCount = whenCount + otherwiseCount;

      router.lineNumber = findLineNumber(element, xmlContent);
      routers.add(router);
    }

    return routers;
  }

  private List<FlowReference> extractFlowReferences(Document doc, List<MuleSoftFlow> flows) {
    List<FlowReference> references = new ArrayList<>();

    NodeList flowRefNodes = doc.getElementsByTagName("flow-ref");
    for (int i = 0; i < flowRefNodes.getLength(); i++) {
      Element element = (Element) flowRefNodes.item(i);
      FlowReference ref = new FlowReference();
      ref.flowName = element.getAttribute("name");
      ref.element = element;

      // Find the source flow containing this flow-ref
      ref.sourceFlowName = findContainingFlowName(element, flows);

      references.add(ref);
    }

    return references;
  }

  private String findContainingFlowName(Element element, List<MuleSoftFlow> flows) {
    Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
      Element parentElement = (Element) parent;
      String tagName = parentElement.getTagName();
      if ("flow".equals(tagName) || "sub-flow".equals(tagName)) {
        return parentElement.getAttribute("name");
      }
      parent = parent.getParentNode();
    }
    return null;
  }

  /**
   * Find the line number of an element in the XML content.
   * Since standard DOM parser doesn't preserve line numbers, we search for the element in the content.
   */
  private int findLineNumber(Element element, String xmlContent) {
    try {
      // Build a unique identifier for this element
      String tagName = element.getTagName();
      String searchPattern = "<" + tagName;

      // Add key attributes to make it more unique
      StringBuilder patternBuilder = new StringBuilder(searchPattern);

      // Check for common identifying attributes
      String[] identifyingAttrs = {"name", "path", "config-ref", "class", "message", "protocol", "host", "port"};
      for (String attr : identifyingAttrs) {
        String attrValue = element.getAttribute(attr);
        if (attrValue != null && !attrValue.isEmpty()) {
          patternBuilder.append(" ").append(attr).append("=\"").append(attrValue).append("\"");
          break; // Use first identifying attribute found
        }
      }

      String uniquePattern = patternBuilder.toString();

      // Find the pattern in the XML content
      int index = xmlContent.indexOf(uniquePattern);
      if (index == -1) {
        // Fallback: just search for the tag name
        index = xmlContent.indexOf(searchPattern);
      }

      if (index >= 0) {
        // Count newlines before this index to get line number
        int lineNumber = 1;
        for (int i = 0; i < index; i++) {
          if (xmlContent.charAt(i) == '\n') {
            lineNumber++;
          }
        }
        return lineNumber;
      }
    } catch (Exception e) {
      LOG.debug("Error finding line number for element", e);
    }

    return 1; // Default to line 1 if not found
  }

  /**
   * Represents a parsed MuleSoft file with all extracted information.
   */
  public static class ParsedMuleSoftFile {
    public String rawContent;
    public Document xmlDocument;
    public List<MuleSoftFlow> flows = new ArrayList<>();
    public List<JavaCodeBlock> javaCodeBlocks = new ArrayList<>();
    public List<JavaInvocation> javaInvocations = new ArrayList<>();
    public List<ErrorHandler> errorHandlers = new ArrayList<>();
    public List<HttpConfiguration> httpConfigurations = new ArrayList<>();
    public List<DatabaseConnector> databaseConnectors = new ArrayList<>();
    public List<LoggerComponent> loggers = new ArrayList<>();
    public List<ChoiceRouter> choiceRouters = new ArrayList<>();
    public List<FlowReference> flowReferences = new ArrayList<>();
  }

  /**
   * Represents a MuleSoft flow or sub-flow.
   */
  public static class MuleSoftFlow {
    public String name;
    public boolean isSubFlow;
    public Element element;
    public int lineNumber = 1;
  }

  /**
   * Represents embedded Java code found in the MuleSoft file.
   */
  public static class JavaCodeBlock {
    public String type; // e.g., "custom-transformer", "component", "inline-java"
    public String className;
    public String code;
    public CompilationUnit compilationUnit;
  }

  /**
   * Represents a Java method/class invocation from MuleSoft XML.
   */
  public static class JavaInvocation {
    public String type; // e.g., "invoke", "new"
    public String className;
    public String methodName;
    public Element element;
  }

  /**
   * Represents an error handler in MuleSoft.
   */
  public static class ErrorHandler {
    public String type; // e.g., "error-handler", "on-error-continue", "on-error-propagate"
    public Element element;
    public boolean isEmpty;
    public int lineNumber = 1;
  }

  /**
   * Represents an HTTP configuration.
   */
  public static class HttpConfiguration {
    public String type; // e.g., "listener", "request", "listener-config", "request-config"
    public String protocol;
    public String host;
    public String port;
    public Element element;
    public boolean hasAuthentication;
    public boolean hasTLS;
    public int lineNumber = 1; // Default to line 1 if not found
  }

  /**
   * Represents a database connector.
   */
  public static class DatabaseConnector {
    public String type; // e.g., "config", "select", "insert", "update"
    public Element element;
    public boolean hasSSL;
    public boolean hasPooling;
    public int lineNumber = 1;
  }

  /**
   * Represents a logger component.
   */
  public static class LoggerComponent {
    public String message;
    public String level;
    public Element element;
    public int lineNumber = 1;
  }

  /**
   * Represents a choice router.
   */
  public static class ChoiceRouter {
    public Element element;
    public int branchCount;
    public int lineNumber = 1;
  }

  /**
   * Represents a flow reference.
   */
  public static class FlowReference {
    public String flowName;
    public Element element;
    public String sourceFlowName;
  }
}
