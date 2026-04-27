package ru.tms.user.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на создание банка вопросов")
public record CreateQuestionBankRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be <= 100 characters")
        @Schema(description = "Название банка вопросов", example = "Математика для 5 класса", required = true, maxLength = 100)
        String name,

        @NotBlank(message = "description must not be blank")
        @Schema(description = "Описание банка вопросов", example = "Банк вопросов по математике для учащихся 5 класса", required = true)
        String description,

        @Schema(description = "Активен ли банк вопросов", example = "true")
        Boolean isActive
) {
}
