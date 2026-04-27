package ru.tms.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Вариант ответа на вопрос")
public record AnswerOptionResponse(
        @Schema(description = "ID варианта ответа", example = "10")
        Long id,
        @Schema(description = "Текст варианта ответа", example = "Скрытие деталей реализации и показ только необходимой информации")
        String optionText,
        @Schema(description = "Правильный ли ответ", example = "true")
        Boolean isCorrect,
        @Schema(description = "Порядок отображения", example = "1")
        Integer displayOrder
) {
}










