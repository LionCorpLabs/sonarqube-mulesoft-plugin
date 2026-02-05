package com.lioncorp.sonar.mulesoft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MuleSoftSensorTest {

    private FileSystem fileSystem;
    private CheckFactory checkFactory;
    private MuleSoftSensor sensor;
    private SensorDescriptor sensorDescriptor;

    @BeforeEach
    void setUp() {
        fileSystem = mock(FileSystem.class);

        // Create CheckFactory using ActiveRules instead of mocking it directly
        // This avoids Byte Buddy issues with Java 21
        ActiveRules activeRules = mock(ActiveRules.class);
        when(activeRules.findAll()).thenReturn(Collections.emptyList());
        checkFactory = new CheckFactory(activeRules);

        sensor = new MuleSoftSensor(fileSystem, checkFactory);
        sensorDescriptor = mock(SensorDescriptor.class);
        when(sensorDescriptor.onlyOnLanguage(anyString())).thenReturn(sensorDescriptor);
        when(sensorDescriptor.name(anyString())).thenReturn(sensorDescriptor);
        when(sensorDescriptor.onlyOnFileType(any())).thenReturn(sensorDescriptor);
    }

    @Test
    void testDescribe() {
        sensor.describe(sensorDescriptor);

        verify(sensorDescriptor).onlyOnLanguage(MuleSoftLanguage.KEY);
        verify(sensorDescriptor).name("MuleSoft Sensor");
        verify(sensorDescriptor).onlyOnFileType(InputFile.Type.MAIN);
    }

    @Test
    void testExecuteWithNoFiles() {
        SensorContext context = mock(SensorContext.class);
        FilePredicates predicates = mock(FilePredicates.class);

        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(any())).thenReturn(Collections.emptyList());

        sensor.execute(context);

        verify(fileSystem).inputFiles(any());
    }

    @Test
    void testExecuteWithMuleSoftFile() throws IOException {
        String muleSoftContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <mule xmlns="http://www.mulesoft.org/schema/mule/core">
                    <flow name="testFlow">
                        <logger message="test"/>
                    </flow>
                </mule>
                """;

        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn(muleSoftContent);
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(java.net.URI.create("file:///test.xml"));

        SensorContext context = mock(SensorContext.class);
        FilePredicates predicates = mock(FilePredicates.class);

        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(inputFile));

        sensor.execute(context);

        verify(inputFile, atLeastOnce()).contents();
    }

    @Test
    void testExecuteWithNonMuleSoftXmlFile() throws IOException {
        String nonMuleSoftContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root>
                    <element>data</element>
                </root>
                """;

        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn(nonMuleSoftContent);
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(java.net.URI.create("file:///test.xml"));

        SensorContext context = mock(SensorContext.class);
        FilePredicates predicates = mock(FilePredicates.class);

        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(inputFile));

        sensor.execute(context);

        // Should skip non-MuleSoft files
        verify(inputFile, atLeastOnce()).contents();
    }

    @Test
    void testExecuteWithMultipleFiles() throws IOException {
        InputFile file1 = mock(InputFile.class);
        InputFile file2 = mock(InputFile.class);

        when(file1.contents()).thenReturn("<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"><flow name=\"f1\"/></mule>");
        when(file1.filename()).thenReturn("file1.xml");
        when(file1.uri()).thenReturn(java.net.URI.create("file:///file1.xml"));

        when(file2.contents()).thenReturn("<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"><flow name=\"f2\"/></mule>");
        when(file2.filename()).thenReturn("file2.xml");
        when(file2.uri()).thenReturn(java.net.URI.create("file:///file2.xml"));

        SensorContext context = mock(SensorContext.class);
        FilePredicates predicates = mock(FilePredicates.class);

        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Arrays.asList(file1, file2));

        sensor.execute(context);

        verify(file1).contents();
        verify(file2).contents();
    }

    @Test
    void testExecuteWithFileReadError() throws IOException {
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenThrow(new IOException("Read error"));
        when(inputFile.filename()).thenReturn("error.xml");
        when(inputFile.uri()).thenReturn(java.net.URI.create("file:///error.xml"));

        SensorContext context = mock(SensorContext.class);
        FilePredicates predicates = mock(FilePredicates.class);

        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(inputFile));

        // Should handle error gracefully
        sensor.execute(context);

        verify(inputFile).uri();
    }

    @Test
    void testSensorCreatesChecks() {
        // Verify sensor was created successfully with CheckFactory
        assertThat(sensor).isNotNull();
    }

    @Test
    void testIsMuleSoftFileWithMuleNamespace() throws IOException {
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn("<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"/>");
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(java.net.URI.create("file:///test.xml"));

        SensorContext context = mock(SensorContext.class);
        FilePredicates predicates = mock(FilePredicates.class);

        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(inputFile));

        sensor.execute(context);

        verify(inputFile, atLeastOnce()).contents();
    }

    @Test
    void testIsMuleSoftFileWithMuleElement() throws IOException {
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn("<mule><flow name=\"test\"/></mule>");
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(java.net.URI.create("file:///test.xml"));

        SensorContext context = mock(SensorContext.class);
        FilePredicates predicates = mock(FilePredicates.class);

        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(inputFile));

        sensor.execute(context);

        verify(inputFile, atLeastOnce()).contents();
    }

    @Test
    void testIsMuleSoftFileWithMuleSoftSchemaUrl() throws IOException {
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn("<?xml version=\"1.0\"?><root xmlns=\"http://www.mulesoft.org/schema/mule/http\"/>");
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(java.net.URI.create("file:///test.xml"));

        SensorContext context = mock(SensorContext.class);
        FilePredicates predicates = mock(FilePredicates.class);

        when(fileSystem.predicates()).thenReturn(predicates);
        when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(inputFile));

        sensor.execute(context);

        verify(inputFile, atLeastOnce()).contents();
    }

    @Test
    void testSensorImplementsInterfaceCorrectly() {
        assertThat(sensor).isInstanceOf(org.sonar.api.batch.sensor.Sensor.class);
    }
}

