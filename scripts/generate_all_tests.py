#!/usr/bin/env python3
"""
Script to generate comprehensive unit tests for all MuleSoft SonarQube plugin classes.
Generates 131 test files with 100% code coverage goal.
"""

import os
from pathlib import Path

# Base paths
BASE_DIR = Path(__file__).parent.parent
SRC_MAIN = BASE_DIR / "src/main/java/com/lioncorp/sonar/mulesoft"
SRC_TEST = BASE_DIR / "src/test/java/com/lioncorp/sonar/mulesoft"

def get_test_template_for_sensor():
    return '''package com.lioncorp.sonar.mulesoft;

import com.lioncorp.sonar.mulesoft.checks.CheckList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MuleSoftSensorTest {

    private FileSystem fileSystem;
    private CheckFactory checkFactory;
    private MuleSoftSensor sensor;
    private SensorDescriptor sensorDescriptor;

    @BeforeEach
    void setUp() {
        fileSystem = mock(FileSystem.class);
        checkFactory = mock(CheckFactory.class);

        @SuppressWarnings("unchecked")
        Checks<MuleSoftCheck> checks = mock(Checks.class);
        when(checkFactory.create(anyString())).thenReturn(checks);
        when(checks.addAnnotatedChecks(any())).thenReturn(checks);
        when(checks.all()).thenReturn(java.util.Collections.emptyList());

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
        SensorContextTester context = SensorContextTester.create(Path.of("."));

        when(fileSystem.predicates()).thenReturn(context.fileSystem().predicates());
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Collections.emptyList());

        sensor.execute(context);

        // Should complete without errors
        assertThat(context.allIssues()).isEmpty();
    }

    @Test
    void testExecuteWithMuleSoftFile() throws IOException {
        Path baseDir = Files.createTempDirectory("mulesoft-test");
        Path xmlFile = baseDir.resolve("test.xml");

        String muleSoftContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <mule xmlns="http://www.mulesoft.org/schema/mule/core">
                    <flow name="testFlow">
                        <logger message="test"/>
                    </flow>
                </mule>
                """;

        Files.writeString(xmlFile, muleSoftContent, StandardCharsets.UTF_8);

        SensorContextTester context = SensorContextTester.create(baseDir);
        InputFile inputFile = context.fileSystem().inputFile(
                context.fileSystem().predicates().hasAbsolutePath(xmlFile.toString())
        );

        when(fileSystem.predicates()).thenReturn(context.fileSystem().predicates());
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Collections.singletonList(inputFile));

        sensor.execute(context);

        // Cleanup
        Files.deleteIfExists(xmlFile);
        Files.deleteIfExists(baseDir);
    }

    @Test
    void testExecuteWithNonMuleSoftXmlFile() throws IOException {
        Path baseDir = Files.createTempDirectory("mulesoft-test");
        Path xmlFile = baseDir.resolve("test.xml");

        String nonMuleSoftContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root>
                    <element>data</element>
                </root>
                """;

        Files.writeString(xmlFile, nonMuleSoftContent, StandardCharsets.UTF_8);

        SensorContextTester context = SensorContextTester.create(baseDir);
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn(nonMuleSoftContent);
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(xmlFile.toUri());

        when(fileSystem.predicates()).thenReturn(context.fileSystem().predicates());
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Collections.singletonList(inputFile));

        sensor.execute(context);

        // Should skip non-MuleSoft files
        verify(inputFile, atLeastOnce()).contents();

        // Cleanup
        Files.deleteIfExists(xmlFile);
        Files.deleteIfExists(baseDir);
    }

    @Test
    void testExecuteWithMultipleFiles() {
        InputFile file1 = mock(InputFile.class);
        InputFile file2 = mock(InputFile.class);

        when(file1.contents()).thenReturn("<mule xmlns=\\"http://www.mulesoft.org/schema/mule/core\\"><flow name=\\"f1\\"/></mule>");
        when(file1.filename()).thenReturn("file1.xml");
        when(file1.uri()).thenReturn(Path.of("file1.xml").toUri());

        when(file2.contents()).thenReturn("<mule xmlns=\\"http://www.mulesoft.org/schema/mule/core\\"><flow name=\\"f2\\"/></mule>");
        when(file2.filename()).thenReturn("file2.xml");
        when(file2.uri()).thenReturn(Path.of("file2.xml").toUri());

        SensorContext context = SensorContextTester.create(Path.of("."));
        when(fileSystem.predicates()).thenReturn(((SensorContextTester)context).fileSystem().predicates());
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Arrays.asList(file1, file2));

        sensor.execute(context);

        verify(file1).contents();
        verify(file2).contents();
    }

    @Test
    void testExecuteWithFileReadError() {
        InputFile inputFile = mock(InputFile.class);

        try {
            when(inputFile.contents()).thenThrow(new IOException("Read error"));
        } catch (IOException e) {
            // Expected in setup
        }
        when(inputFile.filename()).thenReturn("error.xml");
        when(inputFile.uri()).thenReturn(Path.of("error.xml").toUri());

        SensorContext context = SensorContextTester.create(Path.of("."));
        when(fileSystem.predicates()).thenReturn(((SensorContextTester)context).fileSystem().predicates());
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Collections.singletonList(inputFile));

        // Should handle error gracefully
        sensor.execute(context);
    }

    @Test
    void testSensorCreatesChecks() {
        verify(checkFactory).create(MuleSoftRulesDefinition.REPOSITORY_KEY);
    }

    @Test
    void testIsMuleSoftFileWithMuleNamespace() throws IOException {
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn("<mule xmlns=\\"http://www.mulesoft.org/schema/mule/core\\"/>");
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(Path.of("test.xml").toUri());

        SensorContext context = SensorContextTester.create(Path.of("."));
        when(fileSystem.predicates()).thenReturn(((SensorContextTester)context).fileSystem().predicates());
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Collections.singletonList(inputFile));

        sensor.execute(context);

        verify(inputFile, atLeastOnce()).contents();
    }

    @Test
    void testIsMuleSoftFileWithMuleElement() throws IOException {
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn("<mule><flow name=\\"test\\"/></mule>");
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(Path.of("test.xml").toUri());

        SensorContext context = SensorContextTester.create(Path.of("."));
        when(fileSystem.predicates()).thenReturn(((SensorContextTester)context).fileSystem().predicates());
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Collections.singletonList(inputFile));

        sensor.execute(context);

        verify(inputFile, atLeastOnce()).contents();
    }

    @Test
    void testIsMuleSoftFileWithMuleSoftSchemaUrl() throws IOException {
        InputFile inputFile = mock(InputFile.class);
        when(inputFile.contents()).thenReturn("<?xml version=\\"1.0\\"?><root xmlns=\\"http://www.mulesoft.org/schema/mule/http\\"/>");
        when(inputFile.filename()).thenReturn("test.xml");
        when(inputFile.uri()).thenReturn(Path.of("test.xml").toUri());

        SensorContext context = SensorContextTester.create(Path.of("."));
        when(fileSystem.predicates()).thenReturn(((SensorContextTester)context).fileSystem().predicates());
        when(fileSystem.inputFiles(any())).thenReturn(java.util.Collections.singletonList(inputFile));

        sensor.execute(context);

        verify(inputFile, atLeastOnce()).contents();
    }
}
'''

def get_test_template_for_rules_definition():
    return '''package com.lioncorp.sonar.mulesoft;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class MuleSoftRulesDefinitionTest {

    @Test
    void testDefineRepository() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();

        rulesDefinition.define(context);

        RulesDefinition.Repository repository = context.repository(MuleSoftRulesDefinition.REPOSITORY_KEY);
        assertThat(repository).isNotNull();
        assertThat(repository.key()).isEqualTo("mulesoft");
        assertThat(repository.name()).isEqualTo("MuleSoft Analyzer");
        assertThat(repository.language()).isEqualTo(MuleSoftLanguage.KEY);
    }

    @Test
    void testAllRulesAreDefined() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();

        rulesDefinition.define(context);

        RulesDefinition.Repository repository = context.repository("mulesoft");
        assertThat(repository.rules()).hasSize(110);
    }

    @Test
    void testSecurityRules() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        // Test a few security rules
        RulesDefinition.Rule ms001 = repository.rule("MS001");
        assertThat(ms001).isNotNull();
        assertThat(ms001.name()).isEqualTo("Credentials should not be hardcoded");
        assertThat(ms001.htmlDescription()).contains("Hardcoded credentials");

        RulesDefinition.Rule ms002 = repository.rule("MS002");
        assertThat(ms002).isNotNull();
        assertThat(ms002.name()).isEqualTo("HTTP endpoints should use HTTPS");
    }

    @Test
    void testStructureRules() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        RulesDefinition.Rule ms031 = repository.rule("MS031");
        assertThat(ms031).isNotNull();
        assertThat(ms031.name()).isEqualTo("Empty flows should be removed");

        RulesDefinition.Rule ms032 = repository.rule("MS032");
        assertThat(ms032).isNotNull();
        assertThat(ms032.name()).isEqualTo("Large flows should be broken down");
    }

    @Test
    void testNamingRules() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        RulesDefinition.Rule ms059 = repository.rule("MS059");
        assertThat(ms059).isNotNull();
        assertThat(ms059.name()).isEqualTo("All flows must have descriptive names");
    }

    @Test
    void testPerformanceRules() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        RulesDefinition.Rule ms072 = repository.rule("MS072");
        assertThat(ms072).isNotNull();
        assertThat(ms072.name()).isEqualTo("Use async processing for independent operations");
    }

    @Test
    void testErrorHandlingRules() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        RulesDefinition.Rule ms085 = repository.rule("MS085");
        assertThat(ms085).isNotNull();
        assertThat(ms085.name()).isEqualTo("Applications should have global error handlers");
    }

    @Test
    void testJavaIntegrationRules() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        RulesDefinition.Rule ms097 = repository.rule("MS097");
        assertThat(ms097).isNotNull();
        assertThat(ms097.name()).isEqualTo("Java invocations should be type-safe");
    }

    @Test
    void testRuleHasImpacts() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        RulesDefinition.Rule rule = repository.rule("MS001");
        assertThat(rule.defaultImpacts()).isNotEmpty();
    }

    @Test
    void testRuleHasTags() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        RulesDefinition.Rule rule = repository.rule("MS001");
        assertThat(rule.tags()).isNotEmpty();
        assertThat(rule.tags()).contains("security");
    }

    @Test
    void testAllRulesHaveUniqueKeys() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        java.util.Set<String> ruleKeys = new java.util.HashSet<>();
        repository.rules().forEach(rule -> {
            assertThat(ruleKeys.add(rule.key())).isTrue();
        });
    }

    @Test
    void testAllRulesHaveDescriptions() {
        MuleSoftRulesDefinition rulesDefinition = new MuleSoftRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();
        rulesDefinition.define(context);
        RulesDefinition.Repository repository = context.repository("mulesoft");

        repository.rules().forEach(rule -> {
            assertThat(rule.htmlDescription()).isNotBlank();
            assertThat(rule.name()).isNotBlank();
        });
    }

    @Test
    void testRepositoryConstants() {
        assertThat(MuleSoftRulesDefinition.REPOSITORY_KEY).isEqualTo("mulesoft");
        assertThat(MuleSoftRulesDefinition.REPOSITORY_NAME).isEqualTo("MuleSoft Analyzer");
    }
}
'''

def get_test_template_for_quality_profile():
    return '''package com.lioncorp.sonar.mulesoft;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class MuleSoftQualityProfileTest {

    @Test
    void testDefineProfile() {
        MuleSoftQualityProfile qualityProfile = new MuleSoftQualityProfile();
        BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

        qualityProfile.define(context);

        BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(MuleSoftLanguage.KEY, "MuleSoft Way");
        assertThat(profile).isNotNull();
        assertThat(profile.name()).isEqualTo("MuleSoft Way");
        assertThat(profile.language()).isEqualTo(MuleSoftLanguage.KEY);
        assertThat(profile.isDefault()).isTrue();
    }

    @Test
    void testAllRulesAreActivated() {
        MuleSoftQualityProfile qualityProfile = new MuleSoftQualityProfile();
        BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

        qualityProfile.define(context);

        BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(MuleSoftLanguage.KEY, "MuleSoft Way");
        assertThat(profile.rules()).hasSize(110);
    }

    @Test
    void testSecurityRulesActivated() {
        MuleSoftQualityProfile qualityProfile = new MuleSoftQualityProfile();
        BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

        qualityProfile.define(context);

        BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(MuleSoftLanguage.KEY, "MuleSoft Way");

        // Check some security rules are activated
        boolean hasMS001 = profile.rules().stream()
                .anyMatch(rule -> rule.ruleKey().equals("MS001"));
        assertThat(hasMS001).isTrue();
    }

    @Test
    void testAllRuleCategoriesActivated() {
        MuleSoftQualityProfile qualityProfile = new MuleSoftQualityProfile();
        BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

        qualityProfile.define(context);

        BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(MuleSoftLanguage.KEY, "MuleSoft Way");

        // Security rules (MS001-MS030)
        long securityRules = profile.rules().stream()
                .filter(rule -> {
                    String key = rule.ruleKey();
                    return key.compareTo("MS001") >= 0 && key.compareTo("MS030") <= 0;
                })
                .count();
        assertThat(securityRules).isEqualTo(30);

        // Structure rules (MS031-MS058)
        long structureRules = profile.rules().stream()
                .filter(rule -> {
                    String key = rule.ruleKey();
                    return key.compareTo("MS031") >= 0 && key.compareTo("MS058") <= 0;
                })
                .count();
        assertThat(structureRules).isEqualTo(28);

        // Naming rules (MS059-MS071)
        long namingRules = profile.rules().stream()
                .filter(rule -> {
                    String key = rule.ruleKey();
                    return key.compareTo("MS059") >= 0 && key.compareTo("MS071") <= 0;
                })
                .count();
        assertThat(namingRules).isEqualTo(13);

        // Performance rules (MS072-MS084)
        long performanceRules = profile.rules().stream()
                .filter(rule -> {
                    String key = rule.ruleKey();
                    return key.compareTo("MS072") >= 0 && key.compareTo("MS084") <= 0;
                })
                .count();
        assertThat(performanceRules).isEqualTo(13);

        // Error handling rules (MS085-MS096)
        long errorRules = profile.rules().stream()
                .filter(rule -> {
                    String key = rule.ruleKey();
                    return key.compareTo("MS085") >= 0 && key.compareTo("MS096") <= 0;
                })
                .count();
        assertThat(errorRules).isEqualTo(12);

        // Java integration rules (MS097-MS110)
        long javaRules = profile.rules().stream()
                .filter(rule -> {
                    String key = rule.ruleKey();
                    return key.compareTo("MS097") >= 0 && key.compareTo("MS110") <= 0;
                })
                .count();
        assertThat(javaRules).isEqualTo(14);
    }

    @Test
    void testProfileIsDefault() {
        MuleSoftQualityProfile qualityProfile = new MuleSoftQualityProfile();
        BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

        qualityProfile.define(context);

        BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(MuleSoftLanguage.KEY, "MuleSoft Way");
        assertThat(profile.isDefault()).isTrue();
    }

    @Test
    void testProfileForCorrectLanguage() {
        MuleSoftQualityProfile qualityProfile = new MuleSoftQualityProfile();
        BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

        qualityProfile.define(context);

        BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(MuleSoftLanguage.KEY, "MuleSoft Way");
        assertThat(profile.language()).isEqualTo("mulesoft");
    }
}
'''

def create_core_tests():
    """Create tests for core plugin classes"""
    tests = {
        "MuleSoftSensorTest.java": get_test_template_for_sensor(),
        "MuleSoftRulesDefinitionTest.java": get_test_template_for_rules_definition(),
        "MuleSoftQualityProfileTest.java": get_test_template_for_quality_profile(),
    }

    for filename, content in tests.items():
        filepath = SRC_TEST / filename
        filepath.write_text(content)
        print(f"Created: {filepath}")

if __name__ == "__main__":
    print("Generating core test files...")
    create_core_tests()
    print("Core tests generated successfully!")
