package ru.tms.user.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTestRequest(
        Long questionBankId,

        @Size(max = 300, message = "name must be <= 300 characters")
        String name,

        String description,

        String difficulty,

        String status,

        @Min(value = 0, message = "timeLimit must be >= 0")
        Integer timeLimit,

        @Min(value = 0, message = "numQuestions must be >= 0")
        Integer numQuestions,

        @Min(value = 0, message = "attempts must be >= 0")
        Integer attempts,

        LocalDate publishedDate
) {
}














