package ru.tms.user.controller.dto;

import jakarta.validation.constraints.Size;

public record UpdateQuestionBankRequest(
        @Size(max = 100, message = "name must be <= 100 characters")
        String name,
        String description,
        Boolean isActive
) {
}
