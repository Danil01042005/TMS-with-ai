package ru.tms.user.controller.mapper;

import org.springframework.stereotype.Component;
import ru.tms.user.controller.dto.CreateQuestionBankRequest;
import ru.tms.user.controller.dto.QuestionBankResponse;
import ru.tms.user.controller.dto.UpdateQuestionBankRequest;
import ru.tms.user.entity.QuestionBank;
import ru.tms.user.service.command.CreateQuestionBankCommand;
import ru.tms.user.service.command.UpdateQuestionBankCommand;

@Component
public class QuestionBankMapper {

    public CreateQuestionBankCommand toCreateCommand(CreateQuestionBankRequest request) {
        return new CreateQuestionBankCommand(
                request.name(),
                request.description(),
                request.isActive()
        );
    }

    public UpdateQuestionBankCommand toUpdateCommand(UpdateQuestionBankRequest request) {
        return new UpdateQuestionBankCommand(
                request.name(),
                request.description(),
                request.isActive()
        );
    }

    public QuestionBankResponse toResponse(QuestionBank bank) {
        return new QuestionBankResponse(
                bank.getBankId(),
                bank.getName(),
                bank.getDescription(),
                bank.getOwnerId(),
                bank.getIsActive(),
                bank.getCreatedDate()
        );
    }
}