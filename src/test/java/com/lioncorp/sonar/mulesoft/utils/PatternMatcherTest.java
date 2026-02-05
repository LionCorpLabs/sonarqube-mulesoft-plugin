package com.lioncorp.sonar.mulesoft.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatternMatcherTest {

    @Test
    void testClassExists() {
        assertThat(PatternMatcher.class).isNotNull();
    }

    @Test
    void testPublicMethods() {
        assertThat(PatternMatcher.class.getDeclaredMethods().length).isGreaterThan(0);
    }
}
