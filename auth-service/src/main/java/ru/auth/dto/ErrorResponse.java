package ru.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(description = "Стандартный ответ об ошибке")
public class ErrorResponse {
    @Schema(description = "Код ошибки", example = "AUTH_INVALID_CREDENTIALS")
    private String code;
    
    @Schema(description = "Сообщение об ошибке", example = "Invalid username or password")
    private String message;
    
    @Schema(description = "Время возникновения ошибки")
    private Instant timestamp;
    
    @Schema(description = "Путь запроса", example = "/auth/login")
    private String path;
    
    @Schema(description = "Название сервиса", example = "auth-service")
    private String service;
    
    @Schema(description = "ID корреляции для трейсинга")
    private String correlationId;
}
