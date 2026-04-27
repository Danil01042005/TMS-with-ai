package ru.tms.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Запрос на генерацию теста с помощью ИИ")
public class GenerateTestRequest {
    
    @NotNull(message = "questionBankId must not be null")
    @Schema(description = "ID банка вопросов", example = "1", required = true)
    private Long questionBankId;
    
    @Schema(description = "Режим генерации: 'topic' (по теме) или 'material' (по материалу)", example = "topic", required = true)
    @NotBlank(message = "mode must not be blank")
    private String mode;
    
    @Schema(description = "Тема для генерации (используется при mode='topic')", example = "Объектно-ориентированное программирование в Java")
    private String topic;
    
    @Schema(description = "Учебный материал для генерации (используется при mode='material')", example = "Java - это объектно-ориентированный язык программирования...")
    private String material;
    
    @NotBlank(message = "difficulty must not be blank")
    @Schema(description = "Сложность вопросов: 'easy', 'medium', 'hard'", example = "medium", required = true)
    private String difficulty;
    
    @NotNull(message = "questionCount must not be null")
    @Min(value = 1, message = "questionCount must be at least 1")
    @Schema(description = "Количество вопросов для генерации", example = "10", required = true)
    private Integer questionCount;
    
    @NotNull(message = "questionTypes must not be null")
    @Size(min = 1, message = "questionTypes must contain at least one type")
    @Schema(description = "Типы вопросов: 'single-choice', 'multiple-choice', 'short-text', 'true-false'", 
            example = "[\"single-choice\", \"multiple-choice\"]", required = true)
    private List<String> questionTypes;
}





