package ru.tms.user.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringNormalizerTest {

    private final StringNormalizer stringNormalizer = new StringNormalizer();

    @Test
    void normalizeShouldReturnNullForNullInput() {
        assertEquals(null, stringNormalizer.normalize(null));
    }

    @Test
    void normalizeShouldTrimAndCollapseSpaces() {
        assertEquals("hello world", stringNormalizer.normalize("  hello    world  "));
    }

    @Test
    void normalizeRequiredShouldThrowForBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> stringNormalizer.normalizeRequired("   "));
    }
}

