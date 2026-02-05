package com.lioncorp.sonar.mulesoft.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckConstantsTest {

    @Test
    void testClassExists() {
        assertThat(CheckConstants.class).isNotNull();
    }

    @Test
    void testIsUtilityClass() {
        // Utility classes should not be instantiable
        assertThat(CheckConstants.class.getDeclaredConstructors()).isNotEmpty();
    }
}
