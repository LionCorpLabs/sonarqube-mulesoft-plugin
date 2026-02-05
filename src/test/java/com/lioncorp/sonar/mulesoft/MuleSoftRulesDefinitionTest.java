package com.lioncorp.sonar.mulesoft;

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
        // MS001 has tags including CWE and OWASP references
        assertThat(rule.tags()).containsAnyOf("cwe", "owasp-a2", "security");
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
