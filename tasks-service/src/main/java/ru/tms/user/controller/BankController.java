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
import ru.tms.user.controller.dto.CreateQuestionBankRequest;
import ru.tms.user.controller.dto.QuestionBankResponse;
import ru.tms.user.controller.dto.UpdateQuestionBankRequest;
import ru.tms.user.controller.dto.ErrorResponse;
import ru.tms.user.controller.mapper.QuestionBankMapper;
import ru.tms.user.controller.support.OwnerIdResolver;
import ru.tms.user.controller.support.SearchQueryNormalizer;
import ru.tms.user.service.QuestionBankService;
import ru.tms.user.entity.QuestionBank;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/tasks-service/banks")
@RequiredArgsConstructor
@Tag(name = "Question Banks", description = "API для управления банками вопросов")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class BankController {

    private static final Logger logger = LoggerFactory.getLogger(BankController.class);
    private final QuestionBankService questionBankService;
    private final QuestionBankMapper questionBankMapper;
    private final OwnerIdResolver ownerIdResolver;
    private final SearchQueryNormalizer searchQueryNormalizer;

    @Operation(
            summary = "Получить список банков вопросов",
            description = "Возвращает список банков вопросов пользователя с пагинацией и поиском. Требуется аутентификация."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение списка",
                    content = @Content(schema = @Schema(implementation = QuestionBankResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<QuestionBankResponse>> getBanks(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(required = false, defaultValue = "createdDate") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[BankController] GET /tasks-service/banks - page={}, size={}, search={}, isActive={}, createdFrom={}, createdTo={}, sortBy={}, sortDir={}, ownerId={}",
                page, size, search, isActive, createdFrom, createdTo, sortBy, sortDir, ownerId);
        try {
            LocalDate createdFromDate = parseDate(createdFrom, "createdFrom");
            LocalDate createdToDate = parseDate(createdTo, "createdTo");

            if (createdFromDate != null && createdToDate != null && createdFromDate.isAfter(createdToDate)) {
                throw new IllegalArgumentException("createdFrom must be <= createdTo");
            }

            Sort sort = buildSort(sortBy, sortDir);
            Pageable pageable = PageRequest.of(page, size, sort);

            String normalizedSearch = null;
            if (search != null && !search.trim().isEmpty()) {
                normalizedSearch = searchQueryNormalizer.normalize(search);
                logger.debug("[BankController] GET /banks - Normalized search query: '{}' -> '{}'", search, normalizedSearch);
            }

            Page<QuestionBank> banksPage = questionBankService.searchAndFilterBanks(
                    ownerId,
                    normalizedSearch,
                    isActive,
                    createdFromDate,
                    createdToDate,
                    pageable
            );

            List<QuestionBankResponse> responses = banksPage.getContent()
                    .stream()
                    .map(questionBankMapper::toResponse)
                    .toList();
            logger.info("[BankController] GET /tasks-service/banks - Success: found {} banks (total: {})", 
                    responses.size(), banksPage.getTotalElements());
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(banksPage.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(banksPage.getTotalPages()))
                    .header("X-Page", String.valueOf(banksPage.getNumber()))
                    .header("X-Size", String.valueOf(banksPage.getSize()))
                    .body(responses);
        } catch (Exception e) {
            logger.error("[BankController] GET /tasks-service/banks - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String property = switch (sortBy == null ? "" : sortBy.trim().toLowerCase()) {
            case "", "createddate", "created_date", "created" -> "createdDate";
            case "name" -> "name";
            case "isactive", "is_active", "active" -> "isActive";
            default -> throw new IllegalArgumentException("Unsupported sortBy: " + sortBy);
        };

        Sort.Direction direction = switch (sortDir == null ? "" : sortDir.trim().toLowerCase()) {
            case "", "desc" -> Sort.Direction.DESC;
            case "asc" -> Sort.Direction.ASC;
            default -> throw new IllegalArgumentException("Unsupported sortDir: " + sortDir);
        };

        return Sort.by(direction, property);
    }

    private LocalDate parseDate(String value, String paramName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // fallback to locale-style date used by some browsers/URLs
        }

        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid " + paramName + " format. Use yyyy-MM-dd or dd.MM.yyyy");
        }
    }

    @Operation(
            summary = "Получить банк вопросов по ID",
            description = "Возвращает банк вопросов по ID. Доступ только к собственным банкам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение банка",
                    content = @Content(schema = @Schema(implementation = QuestionBankResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Банк не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{bankId}")
    public ResponseEntity<QuestionBankResponse> getBank(@PathVariable Long bankId,
                                                        JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[BankController] GET /tasks-service/banks/{} - ownerId={}", bankId, ownerId);
        try {
            QuestionBankResponse response = questionBankMapper.toResponse(
                    questionBankService.findOwnedBank(bankId, ownerId)
            );
            logger.info("[BankController] GET /tasks-service/banks/{} - Success: bank found", bankId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[BankController] GET /tasks-service/banks/{} - Error: {}", bankId, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Создать новый банк вопросов",
            description = "Создает новый банк вопросов для текущего пользователя."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Банк успешно создан",
                    content = @Content(schema = @Schema(implementation = QuestionBankResponse.class))
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
            )
    })
    @PostMapping
    public ResponseEntity<QuestionBankResponse> createBank(@Valid @RequestBody CreateQuestionBankRequest request,
                                                           JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[BankController] POST /tasks-service/banks - ownerId={}, name={}", ownerId, request.name());
        try {
            QuestionBankResponse response = questionBankMapper.toResponse(
                    questionBankService.createBank(ownerId, questionBankMapper.toCreateCommand(request))
            );
            logger.info("[BankController] POST /tasks-service/banks - Success: bank created (id: {}, name: {})",
                    response.id(), response.name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[BankController] POST /tasks-service/banks - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Обновить банк вопросов",
            description = "Обновляет существующий банк вопросов. Доступ только к собственным банкам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Банк успешно обновлен",
                    content = @Content(schema = @Schema(implementation = QuestionBankResponse.class))
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
                    description = "Банк не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{bankId}")
    public ResponseEntity<QuestionBankResponse> updateBank(@PathVariable Long bankId,
                                                           @Valid @RequestBody UpdateQuestionBankRequest request,
                                                           JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[BankController] PUT /tasks-service/banks/{} - ownerId={}", bankId, ownerId);
        try {
            QuestionBankResponse response = questionBankMapper.toResponse(
                    questionBankService.updateBank(bankId, ownerId, questionBankMapper.toUpdateCommand(request))
            );
            logger.info("[BankController] PUT /tasks-service/banks/{} - Success: bank updated (name: {})",
                    bankId, response.name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[BankController] PUT /tasks-service/banks/{} - Error: {}", bankId, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Удалить банк вопросов",
            description = "Удаляет банк вопросов по ID. Доступ только к собственным банкам."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Банк успешно удален"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Банк не найден или нет доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{bankId}")
    public ResponseEntity<Void> deleteBank(@PathVariable Long bankId,
                                           JwtAuthenticationToken authentication) {
        String ownerId = ownerIdResolver.resolve(authentication);
        logger.info("[BankController] DELETE /tasks-service/banks/{} - ownerId={}", bankId, ownerId);
        try {
            questionBankService.deleteBank(bankId, ownerId);
            logger.info("[BankController] DELETE /tasks-service/banks/{} - Success: bank deleted", bankId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("[BankController] DELETE /tasks-service/banks/{} - Error: {}", bankId, e.getMessage(), e);
            throw e;
        }
    }

}


