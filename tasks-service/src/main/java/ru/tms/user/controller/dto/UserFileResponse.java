package ru.tms.user.controller.dto;

import java.time.Instant;

public record UserFileResponse(
        Long fileId,
        Long bankId,
        Long testId,
        String originalFileName,
        String contentType,
        Long fileSizeBytes,
        Instant createdDate
) {
}

