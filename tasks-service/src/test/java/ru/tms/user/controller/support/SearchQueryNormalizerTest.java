package ru.tms.user.controller.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchQueryNormalizerTest {

    private final SearchQueryNormalizer normalizer = new SearchQueryNormalizer();

    @Test
    void normalizeShouldReturnEmptyStringForNull() {
        assertEquals("", normalizer.normalize(null));
    }

    @Test
    void normalizeShouldTrimCompressSpacesAndLowercase() {
        assertEquals("hello world", normalizer.normalize("   HeLLo   WoRLD   "));
    }
}

