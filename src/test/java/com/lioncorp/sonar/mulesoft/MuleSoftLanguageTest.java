package com.lioncorp.sonar.mulesoft;

import org.junit.jupiter.api.Test;
import org.sonar.api.resources.AbstractLanguage;

import static org.assertj.core.api.Assertions.assertThat;

class MuleSoftLanguageTest {

    @Test
    void testLanguageKey() {
        MuleSoftLanguage language = new MuleSoftLanguage();
        assertThat(language.getKey()).isEqualTo("mulesoft");
        assertThat(MuleSoftLanguage.KEY).isEqualTo("mulesoft");
    }

    @Test
    void testLanguageName() {
        MuleSoftLanguage language = new MuleSoftLanguage();
        assertThat(language.getName()).isEqualTo("MuleSoft");
        assertThat(MuleSoftLanguage.NAME).isEqualTo("MuleSoft");
    }

    @Test
    void testFileSuffixes() {
        MuleSoftLanguage language = new MuleSoftLanguage();
        String[] suffixes = language.getFileSuffixes();

        assertThat(suffixes).isNotNull();
        assertThat(suffixes).hasSize(1);
        assertThat(suffixes).contains("xml");
    }

    @Test
    void testFileSuffixesAreNotModifiable() {
        MuleSoftLanguage language = new MuleSoftLanguage();
        String[] suffixes1 = language.getFileSuffixes();
        String[] suffixes2 = language.getFileSuffixes();

        // Each call should return a new array
        assertThat(suffixes1).containsExactly(suffixes2);
    }

    @Test
    void testLanguageIsAbstractLanguage() {
        MuleSoftLanguage language = new MuleSoftLanguage();
        assertThat(language).isInstanceOf(AbstractLanguage.class);
    }

    @Test
    void testMultipleInstances() {
        MuleSoftLanguage language1 = new MuleSoftLanguage();
        MuleSoftLanguage language2 = new MuleSoftLanguage();

        assertThat(language1.getKey()).isEqualTo(language2.getKey());
        assertThat(language1.getName()).isEqualTo(language2.getName());
        assertThat(language1.getFileSuffixes()).containsExactly(language2.getFileSuffixes());
    }
}
