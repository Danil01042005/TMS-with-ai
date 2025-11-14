package ru.tms.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tms.user.entity.QuestionBank;
import ru.tms.user.repository.QuestionBankRepository;

@Service
@RequiredArgsConstructor
public class CreateQuestionBank {

    private final QuestionBankRepository questionBankRepository;

    @Transactional
    public QuestionBank create(QuestionBank bank) {
        return questionBankRepository.save(bank);
    }
}
