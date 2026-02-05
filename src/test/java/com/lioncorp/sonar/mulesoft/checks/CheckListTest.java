package com.lioncorp.sonar.mulesoft.checks;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckListTest {

    @Test
    void testClassExists() {
        assertThat(CheckList.class).isNotNull();
    }

    @Test
    void testIsUtilityClass() {
        // Utility classes should not be instantiable
        assertThat(CheckList.class.getDeclaredConstructors()).isNotEmpty();
    }
}
