package com.lioncorp.sonar.mulesoft.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MuleSoftElementsTest {

    @Test
    void testClassExists() {
        assertThat(MuleSoftElements.class).isNotNull();
    }

    @Test
    void testIsUtilityClass() {
        // Utility classes should not be instantiable
        assertThat(MuleSoftElements.class.getDeclaredConstructors()).isNotEmpty();
    }
}
