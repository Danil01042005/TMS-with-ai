package ru.tms.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.tms.user.entity.enums.DifficultyLevel;
import ru.tms.user.entity.enums.TestStatus;
import ru.tms.user.controller.dto.CreateTestRequest;
import ru.tms.user.controller.dto.GenerateTestRequest;
import ru.tms.user.controller.dto.QuestionResponse;
import ru.tms.user.controller.dto.TestResponse;
import ru.tms.user.controller.dto.UpdateTestRequest;
import ru.tms.user.controller.dto.ErrorResponse;
import ru.tms.user.controller.mapper.QuestionMapper;
import ru.tms.user.controller.mapper.TestMapper;
import ru.tms.user.controller.support.OwnerIdResolver;
import ru.tms.user.service.TestService;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/tasks-service/tests")
@RequiredArgsConstructor
@Tag(name = "Tests", description = "API для управления тестами")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    private final TestService testService;
    private final TestMapper testMapper;
    private final QuestionMapper questionMapper;
    private final OwnerIdResolver ownerIdResolver;

    @Operation(
            summary = "Получить список всех тестов",
            description = "Возвращает список всех тестов текущего пользователя. Требуется аутентификация."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение списка",
                    content = @Content(schema = @Schema(implementation = TestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<TestResponse>> getTests(JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[TestController] GET /tasks-service/tests - ownerId={}", ownerId);
        try {
            List<TestResponse> responses = testService.findAllForOwner(ownerId)
                    .stream()
                    .map(testMapper::toResponse)
                    .toList();
            logger.info("[TestController] GET /tasks-service/tests - Success: found {} tests", responses.size());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("[TestController] GET /tasks-service/tests - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Получить тесты по банку вопросов",
            description = "Возвращает список тестов для указанного банка вопросов. Доступ только к собственным тестам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение списка",
                    content = @Content(schema = @Schema(implementation = TestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Банк вопросов не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/bank/{bankId}")
    public ResponseEntity<List<TestResponse>> getTestsByBank(@PathVariable Long bankId,
                                                             JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[TestController] GET /tasks-service/tests/bank/{} - ownerId={}", bankId, ownerId);
        try {
            List<TestResponse> responses = testService.findAllByBank(bankId, ownerId)
                    .stream()
                    .map(testMapper::toResponse)
                    .toList();
            logger.info("[TestController] GET /tasks-service/tests/bank/{} - Success: found {} tests", bankId, responses.size());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("[TestController] GET /tasks-service/tests/bank/{} - Error: {}", bankId, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Получить тесты по банку (фильтрация, сортировка, пагинация)",
            description = "Те же права, что и у списка по банку; поддерживает поиск по названию/описанию, фильтры по статусу и сложности."
    )
    @GetMapping("/bank/{bankId}/page")
    public ResponseEntity<Page<TestResponse>> getTestsByBankPage(
            @PathVariable Long bankId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        Sort sort = buildTestSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TestResponse> result = testService.searchTestsByBank(bankId, ownerId, search, status, difficulty, pageable)
                .map(testMapper::toResponse);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Получить тест по ID",
            description = "Возвращает тест по ID. Доступ только к собственным тестам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение теста",
                    content = @Content(schema = @Schema(implementation = TestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тест не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{testId}")
    public ResponseEntity<TestResponse> getTest(@PathVariable Long testId,
                                                JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[TestController] GET /tasks-service/tests/{} - ownerId={}", testId, ownerId);
        TestResponse response = testMapper.toResponse(testService.findTestForOwner(testId, ownerId));
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получить вопросы теста",
            description = "Возвращает список вопросов (с вариантами ответов) для указанного теста. Доступ только к собственным тестам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение списка",
                    content = @Content(schema = @Schema(implementation = QuestionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тест не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{testId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(@PathVariable Long testId,
                                                               JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[TestController] GET /tasks-service/tests/{}/questions - ownerId={}", testId, ownerId);
        List<QuestionResponse> responses = testService.findQuestionsForTest(testId, ownerId)
                .stream()
                .sorted(java.util.Comparator.comparing(
                        ru.tms.user.entity.QuestionEntity::getQuestionOrder,
                        java.util.Comparator.nullsLast(Integer::compareTo)
                ))
                .map(questionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Создать новый тест",
            description = "Создает новый тест для указанного банка вопросов. Доступ только к собственным банкам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Тест успешно создан",
                    content = @Content(schema = @Schema(implementation = TestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные данные запроса или банк вопросов не найден/нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<TestResponse> createTest(@Valid @RequestBody CreateTestRequest request,
                                                   JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[TestController] POST /tasks-service/tests - ownerId={}, name={}, bankId={}", 
                ownerId, request.name(), request.questionBankId());
        try {
            TestResponse response = testMapper.toResponse(
                    testService.createTest(ownerId, testMapper.toCreateCommand(request))
            );
            logger.info("[TestController] POST /tasks-service/tests - Success: test created (id: {}, name: {})",
                    response.id(), response.name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[TestController] POST /tasks-service/tests - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Обновить тест",
            description = "Обновляет существующий тест. Доступ только к собственным тестам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Тест успешно обновлен",
                    content = @Content(schema = @Schema(implementation = TestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные данные запроса",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тест не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{testId}")
    public ResponseEntity<TestResponse> updateTest(@PathVariable Long testId,
                                                   @Valid @RequestBody UpdateTestRequest request,
                                                   JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[TestController] PUT /tasks-service/tests/{} - ownerId={}", testId, ownerId);
        try {
            TestResponse response = testMapper.toResponse(
                    testService.updateTest(testId, ownerId, testMapper.toUpdateCommand(request))
            );
            logger.info("[TestController] PUT /tasks-service/tests/{} - Success: test updated (name: {})",
                    testId, response.name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[TestController] PUT /tasks-service/tests/{} - Error: {}", testId, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Удалить тест",
            description = "Удаляет тест по ID. Доступ только к собственным тестам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Тест успешно удален"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тест не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long testId,
                                           JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[TestController] DELETE /tasks-service/tests/{} - ownerId={}", testId, ownerId);
        try {
            testService.deleteTest(testId, ownerId);
            logger.info("[TestController] DELETE /tasks-service/tests/{} - Success: test deleted", testId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("[TestController] DELETE /tasks-service/tests/{} - Error: {}", testId, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Сгенерировать тест с вопросами через ИИ",
            description = "Создает новый тест с вопросами, сгенерированными через ИИ на основе темы или учебного материала. Доступ только к собственным банкам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Тест успешно создан с вопросами",
                    content = @Content(schema = @Schema(implementation = TestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные данные запроса",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Банк вопросов не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка генерации вопросов через ИИ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/generate")
    public ResponseEntity<TestResponse> generateTest(@Valid @RequestBody GenerateTestRequest request,
                                                     JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[TestController] POST /tasks-service/tests/generate - ownerId={}, mode={}, questionCount={}",
                ownerId, request.getMode(), request.getQuestionCount());
        try {
            TestResponse response = testMapper.toResponse(
                    testService.generateTestWithQuestions(
                            ownerId,
                            request.getQuestionBankId(),
                            request.getMode(),
                            request.getTopic(),
                            request.getMaterial(),
                            request.getDifficulty(),
                            request.getQuestionCount(),
                            request.getQuestionTypes()
                    )
            );
            logger.info("[TestController] POST /tasks-service/tests/generate - Success: test created (id: {}, name: {})",
                    response.id(), response.name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[TestController] POST /tasks-service/tests/generate - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Sort buildTestSort(String sortBy, String sortDir) {
        String property = switch (sortBy == null ? "" : sortBy.trim().toLowerCase()) {
            case "", "name" -> "name";
            case "createddate", "created_date", "created" -> "createdDate";
            case "status" -> "status";
            case "difficulty" -> "difficulty";
            case "timelimit", "time_limit" -> "timeLimit";
            case "numquestions", "num_questions", "questions" -> "numQuestions";
            case "testid", "id" -> "testId";
            default -> throw new IllegalArgumentException("Unsupported sortBy: " + sortBy);
        };

        Sort.Direction direction = switch (sortDir == null ? "" : sortDir.trim().toLowerCase()) {
            case "", "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new IllegalArgumentException("Unsupported sortDir: " + sortDir);
        };

        return Sort.by(direction, property);
    }

}


