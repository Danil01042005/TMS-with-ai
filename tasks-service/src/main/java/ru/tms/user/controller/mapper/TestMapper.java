package ru.tms.user.controller.mapper;

import org.springframework.stereotype.Component;
import ru.tms.user.controller.dto.CreateTestRequest;
import ru.tms.user.controller.dto.TestResponse;
import ru.tms.user.controller.dto.UpdateTestRequest;
import ru.tms.user.entity.TestEntity;
import ru.tms.user.service.command.CreateTestCommand;
import ru.tms.user.service.command.UpdateTestCommand;

@Component
public class TestMapper {

    public CreateTestCommand toCreateCommand(CreateTestRequest request) {
        return new CreateTestCommand(
                request.questionBankId(),
                request.name(),
                request.description(),
                request.difficulty(),
                request.status(),
                request.timeLimit(),
                request.numQuestions(),
                request.attempts(),
                request.publishedDate()
        );
    }

    public UpdateTestCommand toUpdateCommand(UpdateTestRequest request) {
        return new UpdateTestCommand(
                request.questionBankId(),
                request.name(),
                request.description(),
                request.difficulty(),
                request.status(),
                request.timeLimit(),
                request.numQuestions(),
                request.attempts(),
                request.publishedDate()
        );
    }

    public TestResponse toResponse(TestEntity test) {
        return new TestResponse(
                test.getTestId(),
                test.getQuestionBank().getBankId(),
                test.getName(),
                test.getDescription(),
                test.getDifficulty(),
                test.getTimeLimit(),
                test.getNumQuestions(),
                test.getAttempts(),
                test.getStatus(),
                test.getCreatedDate(),
                test.getPublishedDate(),
                test.getOwnerId()
        );
    }
}




