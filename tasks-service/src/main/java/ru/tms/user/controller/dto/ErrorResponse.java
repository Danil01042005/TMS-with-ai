package ru.tms.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Стандартный ответ об ошибке")
public record ErrorResponse(
        @Schema(description = "Код ошибки", example = "RESOURCE_NOT_FOUND")
        String code,
        @Schema(description = "Сообщение об ошибке", example = "Resource not found")
        String message
) {
}


