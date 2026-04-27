package ru.tms.user.controller.support;

import org.springframework.stereotype.Component;

@Component
public class SearchQueryNormalizer {

    public String normalize(String query) {
        if (query == null) {
            return "";
        }
        return query.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
}




