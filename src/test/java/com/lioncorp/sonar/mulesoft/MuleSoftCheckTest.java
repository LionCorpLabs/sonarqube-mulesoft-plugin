package com.lioncorp.sonar.mulesoft;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MuleSoftCheckTest {

    @Test
    void testInterfaceExists() {
        assertThat(MuleSoftCheck.class).isNotNull();
    }

    @Test
    void testIsInterface() {
        assertThat(MuleSoftCheck.class.isInterface()).isTrue();
    }
}
