package ru.tms.user.service.command;

public record CreateQuestionBankCommand(
        String name,
        String description,
        Boolean isActive
) {
}




