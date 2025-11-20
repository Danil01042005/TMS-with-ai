package ru.tms.user.controller.dto;

import ru.tms.user.entity.enums.DifficultyLevel;
import ru.tms.user.entity.enums.TestStatus;

import java.time.LocalDate;

public record TestResponse(
        Long id,
        Long questionBankId,
        String name,
        String description,
        DifficultyLevel difficulty,
        Integer timeLimit,
        Integer numQuestions,
        Integer attempts,
        TestStatus status,
        LocalDate createdDate,
        LocalDate publishedDate,
        String ownerId
) {
}




