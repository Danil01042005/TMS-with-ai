package ru.tms.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.tms.user.entity.enums.DifficultyLevel;
import ru.tms.user.entity.enums.QuestionType;

import java.util.List;

@Schema(description = "Вопрос теста")
public record QuestionResponse(
        @Schema(description = "ID вопроса", example = "101")
        Long id,
        @Schema(description = "ID теста", example = "94")
        Long testId,
        @Schema(description = "Текст вопроса", example = "Что такое абстракция в ООП?")
        String questionText,
        @Schema(description = "Тип вопроса", example = "SINGLE_CHOICE")
        QuestionType questionType,
        @Schema(description = "Сложность вопроса", example = "MEDIUM")
        DifficultyLevel difficulty,
        @Schema(description = "Порядок вопроса в тесте", example = "1")
        Integer questionOrder,
        @Schema(description = "Варианты ответа (для SINGLE_CHOICE/MULTIPLE_CHOICE)")
        List<AnswerOptionResponse> answerOptions
) {
}










