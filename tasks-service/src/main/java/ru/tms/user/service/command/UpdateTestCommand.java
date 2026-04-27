package ru.tms.user.service.command;

import java.time.LocalDate;

public record UpdateTestCommand(
        Long questionBankId,
        String name,
        String description,
        String difficulty,
        String status,
        Integer timeLimit,
        Integer numQuestions,
        Integer attempts,
        LocalDate publishedDate
) {
}




