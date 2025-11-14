package ru.tms.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tms.user.entity.QuestionBank;
import ru.tms.user.service.CreateQuestionBank;

@RestController
@RequestMapping("/banks")
@RequiredArgsConstructor
public class BankController {

    private final CreateQuestionBank createQuestionBank;

    @PostMapping("/create")
    public ResponseEntity<QuestionBank> createBank(@RequestBody QuestionBank bank) {
        QuestionBank saved = createQuestionBank.create(bank);
        return ResponseEntity.ok(saved);
    }
}
