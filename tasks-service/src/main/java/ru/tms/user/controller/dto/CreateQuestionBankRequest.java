package ru.tms.user.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionBankRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be <= 100 characters")
        String name,

        @NotBlank(message = "description must not be blank")
        String description,

        Boolean isActive
) {
}
