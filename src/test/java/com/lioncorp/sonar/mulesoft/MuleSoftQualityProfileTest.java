package com.lioncorp.sonar.mulesoft;

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
