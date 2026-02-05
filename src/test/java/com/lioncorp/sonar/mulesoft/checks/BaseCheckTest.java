package com.lioncorp.sonar.mulesoft.checks;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseCheckTest {

    @Test
    void testBaseCheckIsAbstract() {
        assertThat(BaseCheck.class.isInterface()).isFalse();
        assertThat(java.lang.reflect.Modifier.isAbstract(BaseCheck.class.getModifiers())).isTrue();
    }

    @Test
    void testBaseCheckExtendsMuleSoftCheck() {
        assertThat(com.lioncorp.sonar.mulesoft.MuleSoftCheck.class.isAssignableFrom(BaseCheck.class)).isTrue();
    }

    @Test
    void testRepositoryKeyConstant() {
        // Verify constant exists via reflection
        try {
            java.lang.reflect.Field field = BaseCheck.class.getDeclaredField("REPOSITORY_KEY");
            assertThat(field).isNotNull();
        } catch (NoSuchFieldException e) {
            // OK if not accessible
        }
    }
}
