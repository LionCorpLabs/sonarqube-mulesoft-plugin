package com.lioncorp.sonar.mulesoft.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XmlUtilsTest {

    @Test
    void testClassExists() {
        assertThat(XmlUtils.class).isNotNull();
    }

    @Test
    void testPublicMethods() {
        assertThat(XmlUtils.class.getDeclaredMethods().length).isGreaterThan(0);
    }
}
