package com.lioncorp.sonar.mulesoft;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MuleSoftPluginTest {

    @Test
    void testDefineExtensions() {
        MuleSoftPlugin plugin = new MuleSoftPlugin();
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        when(sonarRuntime.getApiVersion()).thenReturn(Version.create(10, 7));
        Plugin.Context context = new Plugin.Context(sonarRuntime);

        plugin.define(context);

        assertThat(context.getExtensions()).hasSize(4);
        assertThat(context.getExtensions()).containsExactlyInAnyOrder(
                MuleSoftLanguage.class,
                MuleSoftSensor.class,
                MuleSoftRulesDefinition.class,
                MuleSoftQualityProfile.class
        );
    }

    @Test
    void testDefineWithDifferentSonarVersions() {
        MuleSoftPlugin plugin = new MuleSoftPlugin();

        // Test with SonarQube 9.9
        SonarRuntime runtime9 = mock(SonarRuntime.class);
        when(runtime9.getApiVersion()).thenReturn(Version.create(9, 9));
        Plugin.Context context9 = new Plugin.Context(runtime9);
        plugin.define(context9);
        assertThat(context9.getExtensions()).hasSize(4);

        // Test with SonarQube 10.7
        SonarRuntime runtime10 = mock(SonarRuntime.class);
        when(runtime10.getApiVersion()).thenReturn(Version.create(10, 7));
        Plugin.Context context10 = new Plugin.Context(runtime10);
        plugin.define(context10);
        assertThat(context10.getExtensions()).hasSize(4);
    }

    @Test
    void testPluginInstance() {
        MuleSoftPlugin plugin = new MuleSoftPlugin();
        assertThat(plugin).isNotNull();
        assertThat(plugin).isInstanceOf(Plugin.class);
    }

    @Test
    void testMultipleDefineCalls() {
        MuleSoftPlugin plugin = new MuleSoftPlugin();
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        when(sonarRuntime.getApiVersion()).thenReturn(Version.create(10, 7));

        Plugin.Context context1 = new Plugin.Context(sonarRuntime);
        plugin.define(context1);
        assertThat(context1.getExtensions()).hasSize(4);

        Plugin.Context context2 = new Plugin.Context(sonarRuntime);
        plugin.define(context2);
        assertThat(context2.getExtensions()).hasSize(4);
    }

    @Test
    void testExtensionTypes() {
        MuleSoftPlugin plugin = new MuleSoftPlugin();
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        when(sonarRuntime.getApiVersion()).thenReturn(Version.create(10, 7));
        Plugin.Context context = new Plugin.Context(sonarRuntime);

        plugin.define(context);

        // Verify all extensions are class types
        context.getExtensions().forEach(extension -> {
            assertThat(extension).isInstanceOf(Class.class);
        });
    }
}
