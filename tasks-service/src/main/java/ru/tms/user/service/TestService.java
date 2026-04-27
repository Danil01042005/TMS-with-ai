package ru.tms.user.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tms.user.entity.QuestionBank;
import ru.tms.user.entity.QuestionEntity;
import ru.tms.user.entity.TestEntity;
import ru.tms.user.entity.enums.DifficultyLevel;
import ru.tms.user.entity.enums.TestStatus;
import ru.tms.user.repository.QuestionBankRepository;
import ru.tms.user.repository.QuestionRepository;
import ru.tms.user.repository.TestRepository;
import ru.tms.user.service.command.CreateTestCommand;
import ru.tms.user.service.command.UpdateTestCommand;
import ru.tms.user.service.exception.NotFoundOrAccessDeniedException;
import ru.tms.user.util.StringNormalizer;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private static final Logger logger = LoggerFactory.getLogger(TestService.class);
    private final TestRepository testRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionRepository questionRepository;
    private final StringNormalizer stringNormalizer;
    private final AIGenerationService aiGenerationService;

    @Transactional
    public TestEntity createTest(String ownerId, CreateTestCommand command) {
        logger.info("[TestService] createTest - ownerId={}, name={}, bankId={}",
                ownerId, command.name(), command.questionBankId());
        QuestionBank bank = questionBankRepository.findByBankIdAndOwnerId(command.questionBankId(), ownerId)
                .orElseThrow(() -> {
                    logger.warn("[TestService] createTest - Bank not found or access denied: bankId={}, ownerId={}", 
                            command.questionBankId(), ownerId);
                    return new NotFoundOrAccessDeniedException("Question bank not found or access denied");
                });

        // Нормализация полей
        String normalizedName = stringNormalizer.normalizeRequired(command.name());
        String normalizedDescription = stringNormalizer.normalize(command.description());
        
        logger.debug("[TestService] createTest - Normalized: name='{}' -> '{}', description='{}' -> '{}'", 
                command.name(), normalizedName, command.description(), normalizedDescription);

        TestEntity test = new TestEntity();
        test.setQuestionBank(bank);
        test.setOwnerId(ownerId);
        test.setName(normalizedName);
        test.setDescription(normalizedDescription);
        test.setDifficulty(parseDifficulty(command.difficulty()));
        test.setStatus(parseStatus(command.status()));
        test.setTimeLimit(command.timeLimit());
        test.setNumQuestions(command.numQuestions());
        test.setAttempts(command.attempts());
        test.setPublishedDate(command.publishedDate());
        TestEntity saved = testRepository.save(test);
        logger.info("[TestService] createTest - Success: test created with id={}", saved.getTestId());
        return saved;
    }

    @Transactional
    public TestEntity updateTest(Long testId, String ownerId, UpdateTestCommand command) {
        logger.info("[TestService] updateTest - testId={}, ownerId={}", testId, ownerId);
        TestEntity test = testRepository.findByTestIdAndOwnerId(testId, ownerId)
                .orElseThrow(() -> {
                    logger.warn("[TestService] updateTest - Test not found or access denied: testId={}, ownerId={}", 
                            testId, ownerId);
                    return new NotFoundOrAccessDeniedException("Test not found or access denied");
                });

        if (command.questionBankId() != null && !command.questionBankId().equals(test.getQuestionBank().getBankId())) {
            logger.debug("[TestService] updateTest - Changing bank: {} -> {}", 
                    test.getQuestionBank().getBankId(), command.questionBankId());
            QuestionBank bank = questionBankRepository.findByBankIdAndOwnerId(command.questionBankId(), ownerId)
                    .orElseThrow(() -> {
                        logger.warn("[TestService] updateTest - Bank not found or access denied: bankId={}, ownerId={}", 
                                command.questionBankId(), ownerId);
                        return new NotFoundOrAccessDeniedException("Question bank not found or access denied");
                    });
            test.setQuestionBank(bank);
        }
        if (command.name() != null) {
            String normalizedName = stringNormalizer.normalizeRequired(command.name());
            logger.debug("[TestService] updateTest - updating name: '{}' -> '{}'", test.getName(), normalizedName);
            test.setName(normalizedName);
        }
        if (command.description() != null) {
            String normalizedDescription = stringNormalizer.normalize(command.description());
            logger.debug("[TestService] updateTest - updating description: '{}' -> '{}'", 
                    test.getDescription(), normalizedDescription);
            test.setDescription(normalizedDescription);
        }
        if (command.difficulty() != null) {
            logger.debug("[TestService] updateTest - updating difficulty: {} -> {}", test.getDifficulty(), command.difficulty());
            test.setDifficulty(parseDifficulty(command.difficulty()));
        }
        if (command.status() != null) {
            logger.debug("[TestService] updateTest - updating status: {} -> {}", test.getStatus(), command.status());
            test.setStatus(parseStatus(command.status()));
        }
        if (command.timeLimit() != null) {
            logger.debug("[TestService] updateTest - updating timeLimit: {} -> {}", test.getTimeLimit(), command.timeLimit());
            test.setTimeLimit(command.timeLimit());
        }
        if (command.numQuestions() != null) {
            logger.debug("[TestService] updateTest - updating numQuestions: {} -> {}", test.getNumQuestions(), command.numQuestions());
            test.setNumQuestions(command.numQuestions());
        }
        if (command.attempts() != null) {
            logger.debug("[TestService] updateTest - updating attempts: {} -> {}", test.getAttempts(), command.attempts());
            test.setAttempts(command.attempts());
        }
        if (command.publishedDate() != null) {
            logger.debug("[TestService] updateTest - updating publishedDate: {} -> {}", test.getPublishedDate(), command.publishedDate());
            test.setPublishedDate(command.publishedDate());
        }
        TestEntity saved = testRepository.save(test);
        logger.info("[TestService] updateTest - Success: test updated (id={})", saved.getTestId());
        return saved;
    }

    @Transactional
    public void deleteTest(Long testId, String ownerId) {
        logger.info("[TestService] deleteTest - testId={}, ownerId={}", testId, ownerId);
        TestEntity test = testRepository.findByTestIdAndOwnerId(testId, ownerId)
                .orElseThrow(() -> {
                    logger.warn("[TestService] deleteTest - Test not found or access denied: testId={}, ownerId={}", 
                            testId, ownerId);
                    return new NotFoundOrAccessDeniedException("Test not found or access denied");
                });
        testRepository.delete(test);
        logger.info("[TestService] deleteTest - Success: test deleted (id={}, name={})", testId, test.getName());
    }

    @Transactional
    public List<TestEntity> findAllForOwner(String ownerId) {
        logger.debug("[TestService] findAllForOwner - ownerId={}", ownerId);
        List<TestEntity> result = testRepository.findAllByOwnerId(ownerId);
        logger.debug("[TestService] findAllForOwner - Found {} tests", result.size());
        return result;
    }

    @Transactional
    public List<TestEntity> findAllByBank(Long bankId, String ownerId) {
        logger.debug("[TestService] findAllByBank - bankId={}, ownerId={}", bankId, ownerId);
        List<TestEntity> result = testRepository.findAllByQuestionBank_BankIdAndOwnerId(bankId, ownerId);
        logger.debug("[TestService] findAllByBank - Found {} tests for bank {}", result.size(), bankId);
        return result;
    }

    @Transactional(readOnly = true)
    public Page<TestEntity> searchTestsByBank(
            Long bankId,
            String ownerId,
            String search,
            TestStatus status,
            DifficultyLevel difficulty,
            Pageable pageable
    ) {
        questionBankRepository.findByBankIdAndOwnerId(bankId, ownerId)
                .orElseThrow(() -> new NotFoundOrAccessDeniedException("Question bank not found or access denied"));

        Specification<TestEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("questionBank").get("bankId"), bankId),
                cb.equal(root.get("ownerId"), ownerId)
        );

        if (search != null && !search.trim().isEmpty()) {
            String normalized = stringNormalizer.normalize(search.trim());
            if (normalized != null && !normalized.isEmpty()) {
                String term = "%" + normalized.toLowerCase() + "%";
                Specification<TestEntity> searchSpec = (root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("name")), term),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), cb.literal(""))), term)
                );
                spec = spec.and(searchSpec);
            }
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (difficulty != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("difficulty"), difficulty));
        }

        return testRepository.findAll(spec, pageable);
    }

    @Transactional
    public List<QuestionEntity> findQuestionsForTest(Long testId, String ownerId) {
        logger.debug("[TestService] findQuestionsForTest - testId={}, ownerId={}", testId, ownerId);
        // Важно: НЕ делаем fetch join сразу tests.questions и questions.answerOptions,
        // иначе Hibernate может упасть на "cannot simultaneously fetch multiple bags".
        // Проверяем доступ по тесту и отдельно грузим вопросы с answerOptions.
        testRepository.findByTestIdAndOwnerId(testId, ownerId)
                .orElseThrow(() -> new NotFoundOrAccessDeniedException("Test not found or access denied"));

        return questionRepository.findAllByTestIdAndOwnerIdWithOptions(testId, ownerId);
    }

    @Transactional(readOnly = true)
    public TestEntity findTestForOwner(Long testId, String ownerId) {
        logger.debug("[TestService] findTestForOwner - testId={}, ownerId={}", testId, ownerId);
        return testRepository.findByTestIdAndOwnerId(testId, ownerId)
                .orElseThrow(() -> new NotFoundOrAccessDeniedException("Test not found or access denied"));
    }

    private DifficultyLevel parseDifficulty(String value) {
        try {
            return DifficultyLevel.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            logger.error("[TestService] parseDifficulty - Unknown difficulty: {}", value, ex);
            throw new IllegalArgumentException("Unknown difficulty: " + value);
        }
    }

    private TestStatus parseStatus(String value) {
        try {
            return TestStatus.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            logger.error("[TestService] parseStatus - Unknown status: {}", value, ex);
            throw new IllegalArgumentException("Unknown status: " + value);
        }
    }

    @Transactional
    public TestEntity generateTestWithQuestions(
            String ownerId,
            Long questionBankId,
            String mode,
            String topic,
            String material,
            String difficulty,
            int questionCount,
            List<String> questionTypes
    ) {
        logger.info("[TestService] generateTestWithQuestions - ownerId={}, bankId={}, mode={}, questionCount={}",
                ownerId, questionBankId, mode, questionCount);

        QuestionBank bank = questionBankRepository.findByBankIdAndOwnerId(questionBankId, ownerId)
                .orElseThrow(() -> {
                    logger.warn("[TestService] generateTestWithQuestions - Bank not found: bankId={}, ownerId={}",
                            questionBankId, ownerId);
                    return new NotFoundOrAccessDeniedException("Question bank not found or access denied");
                });

        // Создаем тест
        String testName = "topic".equals(mode) 
                ? (topic != null && !topic.trim().isEmpty() ? topic : "ИИ Тест")
                : "ИИ Тест";
        String testDescription = "topic".equals(mode)
                ? "Тест, сгенерированный ИИ на тему: " + (topic != null ? topic : "")
                : "Тест, сгенерированный ИИ на основе учебного материала";

        TestEntity test = new TestEntity();
        test.setQuestionBank(bank);
        test.setOwnerId(ownerId);
        test.setName(stringNormalizer.normalizeRequired(testName));
        test.setDescription(stringNormalizer.normalize(testDescription));
        test.setDifficulty(parseDifficulty(difficulty));
        test.setStatus(TestStatus.DRAFT);
        test.setTimeLimit(questionCount * 2); // Примерно 2 минуты на вопрос
        test.setNumQuestions(questionCount);
        test.setAttempts(3);

        TestEntity savedTest = testRepository.save(test);
        logger.info("[TestService] generateTestWithQuestions - Test created: id={}", savedTest.getTestId());

        // Генерируем вопросы через ИИ
        List<QuestionEntity> questions = aiGenerationService.generateQuestions(
                savedTest, mode, topic, material, difficulty, questionCount, questionTypes
        );

        // Сохраняем вопросы (cascade сохранит и answerOptions)
        savedTest.setQuestions(questions);
        savedTest.setNumQuestions(questions.size());
        TestEntity finalTest = testRepository.save(savedTest);

        logger.info("[TestService] generateTestWithQuestions - Success: test id={}, questions count={}",
                finalTest.getTestId(), questions.size());
        return finalTest;
    }
}


