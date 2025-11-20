package ru.tms.user.controller.dto;

import java.time.LocalDate;

public record QuestionBankResponse(
        Long id,
        String name,
        String description,
        String ownerId,
        Boolean isActive,
        LocalDate createdDate
) {
}
