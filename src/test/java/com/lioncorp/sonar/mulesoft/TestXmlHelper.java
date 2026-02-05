package com.lioncorp.sonar.mulesoft;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * Helper class for creating real XML documents in tests.
 * This avoids Mockito issues with mocking org.w3c.dom.Document in Java 21.
 */
public class TestXmlHelper {

    /**
     * Creates an empty XML document
     */
    public static Document createEmptyDocument() {
        return parseXml("<mule></mule>");
    }

    /**
     * Creates a simple valid MuleSoft XML document
     */
    public static Document createValidMuleSoftDocument() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core"
                  xmlns:http="http://www.mulesoft.org/schema/mule/http"
                  xmlns:doc="http://www.mulesoft.org/schema/mule/documentation">
                <flow name="test-flow" doc:name="Test Flow">
                    <http:listener config-ref="HTTP_Listener_config" path="/test"/>
                    <logger level="INFO" message="Test message"/>
                </flow>
            </mule>
            """;
        return parseXml(xml);
    }

    /**
     * Creates a MuleSoft XML document with a flow containing specified components
     */
    public static Document createDocumentWithFlow(String flowName, String... components) {
        StringBuilder componentsXml = new StringBuilder();
        for (String component : components) {
            componentsXml.append("    ").append(component).append("\n");
        }

        String xml = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core">
                <flow name="%s">
            %s    </flow>
            </mule>
            """, flowName, componentsXml);
        return parseXml(xml);
    }

    /**
     * Parses an XML string into a Document
     */
    public static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a document with hardcoded credentials (for security tests)
     */
    public static Document createDocumentWithHardcodedCredentials() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core"
                  xmlns:http="http://www.mulesoft.org/schema/mule/http">
                <http:request-config name="config">
                    <http:request-connection>
                        <http:authentication>
                            <http:basic-authentication username="admin" password="password123"/>
                        </http:authentication>
                    </http:request-connection>
                </http:request-config>
            </mule>
            """;
        return parseXml(xml);
    }

    /**
     * Creates a document with HTTP endpoint (for security tests)
     */
    public static Document createDocumentWithHTTPEndpoint() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core"
                  xmlns:http="http://www.mulesoft.org/schema/mule/http">
                <http:listener-config name="config">
                    <http:listener-connection protocol="HTTP" host="localhost" port="8081"/>
                </http:listener-config>
            </mule>
            """;
        return parseXml(xml);
    }

    /**
     * Creates a document with SQL query (for security tests)
     */
    public static Document createDocumentWithSQLQuery(String query) {
        String xml = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core"
                  xmlns:db="http://www.mulesoft.org/schema/mule/db">
                <flow name="test-flow">
                    <db:select>
                        <db:sql>%s</db:sql>
                    </db:select>
                </flow>
            </mule>
            """, query);
        return parseXml(xml);
    }

    /**
     * Creates a document with multiple flows (for structure tests)
     */
    public static Document createDocumentWithMultipleFlows(int flowCount) {
        StringBuilder flows = new StringBuilder();
        for (int i = 1; i <= flowCount; i++) {
            flows.append(String.format("    <flow name=\"flow-%d\">\n", i));
            flows.append("        <logger level=\"INFO\" message=\"test\"/>\n");
            flows.append("    </flow>\n");
        }

        String xml = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core">
            %s</mule>
            """, flows);
        return parseXml(xml);
    }

    /**
     * Creates a document with a large flow (for structure tests)
     */
    public static Document createDocumentWithLargeFlow(int componentCount) {
        StringBuilder components = new StringBuilder();
        for (int i = 0; i < componentCount; i++) {
            components.append(String.format("        <logger level=\"INFO\" message=\"message%d\"/>\n", i));
        }

        String xml = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core">
                <flow name="large-flow">
            %s    </flow>
            </mule>
            """, components);
        return parseXml(xml);
    }

    /**
     * Creates a document with error handler
     */
    public static Document createDocumentWithErrorHandler() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core">
                <flow name="test-flow">
                    <logger level="INFO" message="test"/>
                    <error-handler>
                        <on-error-continue type="ANY">
                            <logger level="ERROR" message="error"/>
                        </on-error-continue>
                    </error-handler>
                </flow>
            </mule>
            """;
        return parseXml(xml);
    }

    /**
     * Creates a document without error handler
     */
    public static Document createDocumentWithoutErrorHandler() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core">
                <flow name="test-flow">
                    <logger level="INFO" message="test"/>
                </flow>
            </mule>
            """;
        return parseXml(xml);
    }
}
