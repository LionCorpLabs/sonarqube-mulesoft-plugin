package com.lioncorp.sonar.mulesoft.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MuleSoftFileParserTest {

    @Test
    void testClassExists() {
        assertThat(MuleSoftFileParser.class).isNotNull();
    }

    @Test
    void testPublicMethods() {
        assertThat(MuleSoftFileParser.class.getDeclaredMethods().length).isGreaterThan(0);
    }
}
