package ru.tms.user.controller.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Явная форма ответа вместо {@link Page}, чтобы избежать проблем сериализации Jackson
 * (в т.ч. вложенный {@code pageable}/{@code sort}) и совпасть с контрактом фронта {@code PageDto}.
 */
public record UserFilePageResponse(
        List<UserFileResponse> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
    public static UserFilePageResponse from(Page<UserFileResponse> page) {
        return new UserFilePageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
