package ru.tms.user.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Запрос на создание теста")
public record CreateTestRequest(
        @NotNull(message = "questionBankId must not be null")
        @Schema(description = "ID банка вопросов", example = "1", required = true)
        Long questionBankId,

        @NotBlank(message = "name must not be blank")
        @Size(max = 300, message = "name must be <= 300 characters")
        @Schema(description = "Название теста", example = "Тест по математике", required = true, maxLength = 300)
        String name,

        @Schema(description = "Описание теста", example = "Проверка знаний по алгебре")
        String description,

        @NotBlank(message = "difficulty must not be blank")
        @Schema(description = "Сложность теста", example = "MEDIUM", required = true, allowableValues = {"EASY", "MEDIUM", "HARD"})
        String difficulty,

        @NotBlank(message = "status must not be blank")
        @Schema(description = "Статус теста", example = "DRAFT", required = true, allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
        String status,

        @Min(value = 0, message = "timeLimit must be >= 0")
        @Schema(description = "Лимит времени в минутах", example = "60", minimum = "0")
        Integer timeLimit,

        @Min(value = 0, message = "numQuestions must be >= 0")
        @Schema(description = "Количество вопросов", example = "20", minimum = "0")
        Integer numQuestions,

        @Min(value = 0, message = "attempts must be >= 0")
        @Schema(description = "Количество попыток", example = "3", minimum = "0")
        Integer attempts,

        @Schema(description = "Дата публикации", example = "2024-01-15")
        LocalDate publishedDate
) {
}






