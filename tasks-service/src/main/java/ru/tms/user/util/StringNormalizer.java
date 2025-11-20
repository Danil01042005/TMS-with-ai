package ru.tms.user.util;

import org.springframework.stereotype.Component;

@Component
public class StringNormalizer {

    /**
     * Нормализует строку: убирает пробелы в начале/конце и заменяет множественные пробелы на один.
     * 
     * @param value исходная строка
     * @return нормализованная строка или null, если value == null
     */
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    /**
     * Нормализует строку и проверяет, что она не пустая.
     * 
     * @param value исходная строка
     * @return нормализованная строка
     */
    public String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Поле не может быть пустым");
        }
        return normalized;
    }
}

