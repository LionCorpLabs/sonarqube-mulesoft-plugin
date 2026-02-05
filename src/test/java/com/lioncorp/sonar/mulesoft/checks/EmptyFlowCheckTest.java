package com.lioncorp.sonar.mulesoft.checks;

import com.lioncorp.sonar.mulesoft.parser.MuleSoftFileParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.w3c.dom.Document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.lioncorp.sonar.mulesoft.TestXmlHelper;

class EmptyFlowCheckTest {

    private EmptyFlowCheck check;
    private SensorContext sensorContext;
    private InputFile inputFile;
    private MuleSoftFileParser.ParsedMuleSoftFile parsedFile;

    @BeforeEach
    void setUp() {
        check = new EmptyFlowCheck();
        sensorContext = mock(SensorContext.class);
        inputFile = mock(InputFile.class);
        parsedFile = new MuleSoftFileParser.ParsedMuleSoftFile();

        // Setup mock for issue creation
        NewIssue newIssue = mock(NewIssue.class);
        NewIssueLocation location = mock(NewIssueLocation.class);

        when(sensorContext.newIssue()).thenReturn(newIssue);
        when(newIssue.forRule(any(RuleKey.class))).thenReturn(newIssue);
        when(newIssue.newLocation()).thenReturn(location);
        when(location.on(any(InputFile.class))).thenReturn(location);
        when(location.message(anyString())).thenReturn(location);
        when(newIssue.at(any(NewIssueLocation.class))).thenReturn(newIssue);
    }

    @Test
    void testCheckExists() {
        assertThat(check).isNotNull();
    }

    @Test
    void testScanFileWithNullDocument() {
        parsedFile.xmlDocument = null;
        check.scanFile(sensorContext, inputFile, parsedFile);
        // Should not throw exception
    }

    @Test
    void testScanFileWithEmptyDocument() {
        parsedFile.xmlDocument = TestXmlHelper.createEmptyDocument();
        parsedFile.rawContent = "<root/>";
        check.scanFile(sensorContext, inputFile, parsedFile);
        // Should complete without error
    }

    @Test
    void testScanFileWithMuleSoftContent() {
        String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <mule xmlns="http://www.mulesoft.org/schema/mule/core">
                    <flow name="testFlow">
                        <logger message="test"/>
                    </flow>
                </mule>
                """;

        parsedFile.rawContent = xmlContent;
        MuleSoftFileParser parser = new MuleSoftFileParser();
        parsedFile = parser.parse(xmlContent);

        check.scanFile(sensorContext, inputFile, parsedFile);
    }

    @Test
    void testCheckImplementsMuleSoftCheck() {
        assertThat(check).isInstanceOf(com.lioncorp.sonar.mulesoft.MuleSoftCheck.class);
    }
}
