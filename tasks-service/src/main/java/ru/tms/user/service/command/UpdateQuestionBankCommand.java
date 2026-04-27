package ru.tms.user.service.command;

public record UpdateQuestionBankCommand(
        String name,
        String description,
        Boolean isActive
) {
}




