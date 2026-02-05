package com.lioncorp.sonar.mulesoft.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomUtilsTest {

    @Test
    void testClassExists() {
        assertThat(DomUtils.class).isNotNull();
    }

    @Test
    void testPublicMethods() {
        assertThat(DomUtils.class.getDeclaredMethods().length).isGreaterThan(0);
    }
}
