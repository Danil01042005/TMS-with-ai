package ru.tms.user.controller.mapper;

import org.springframework.stereotype.Component;
import ru.tms.user.controller.dto.AnswerOptionResponse;
import ru.tms.user.controller.dto.QuestionResponse;
import ru.tms.user.entity.AnswerOptionEntity;
import ru.tms.user.entity.QuestionEntity;

import java.util.Comparator;
import java.util.List;

@Component
public class QuestionMapper {

    public QuestionResponse toResponse(QuestionEntity entity) {
        Long testId = entity.getTest() != null ? entity.getTest().getTestId() : null;

        List<AnswerOptionResponse> options = entity.getAnswerOptions() == null
                ? List.of()
                : entity.getAnswerOptions().stream()
                .sorted(Comparator.comparing(AnswerOptionEntity::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toResponse)
                .toList();

        return new QuestionResponse(
                entity.getQuestionId(),
                testId,
                entity.getQuestionText(),
                entity.getQuestionType(),
                entity.getDifficulty(),
                entity.getQuestionOrder(),
                options
        );
    }

    private AnswerOptionResponse toResponse(AnswerOptionEntity entity) {
        return new AnswerOptionResponse(
                entity.getOptionId(),
                entity.getOptionText(),
                entity.getCorrect(),
                entity.getDisplayOrder()
        );
    }
}










