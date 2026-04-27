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
import ru.tms.user.repository.QuestionBankRepository;
import ru.tms.user.service.command.CreateQuestionBankCommand;
import ru.tms.user.service.command.UpdateQuestionBankCommand;
import ru.tms.user.service.exception.NotFoundOrAccessDeniedException;
import ru.tms.user.util.StringNormalizer;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuestionBankService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionBankService.class);
    private final QuestionBankRepository questionBankRepository;
    private final StringNormalizer stringNormalizer;

    @Transactional
    public QuestionBank createBank(String ownerId, CreateQuestionBankCommand command) {
        logger.info("[QuestionBankService] createBank - ownerId={}, name={}", ownerId, command.name());
        
        String normalizedName = stringNormalizer.normalizeRequired(command.name());
        String normalizedDescription = stringNormalizer.normalize(command.description());
        
        logger.debug("[QuestionBankService] createBank - Normalized: name='{}' -> '{}', description='{}' -> '{}'", 
                command.name(), normalizedName, command.description(), normalizedDescription);
        
        QuestionBank bank = new QuestionBank();
        bank.setName(normalizedName);
        bank.setDescription(normalizedDescription);
        bank.setIsActive(command.isActive() != null ? command.isActive() : Boolean.TRUE);
        bank.setOwnerId(ownerId);
        QuestionBank saved = questionBankRepository.save(bank);
        logger.info("[QuestionBankService] createBank - Success: bank created with id={}", saved.getBankId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<QuestionBank> findAllForOwner(String ownerId, Pageable pageable) {
        logger.debug("[QuestionBankService] findAllForOwner - ownerId={}, page={}, size={}", 
                ownerId, pageable.getPageNumber(), pageable.getPageSize());
        Page<QuestionBank> result = questionBankRepository.findByOwnerId(ownerId, pageable);
        logger.debug("[QuestionBankService] findAllForOwner - Found {} banks (total: {})", 
                result.getContent().size(), result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<QuestionBank> searchAndFilterBanks(
            String ownerId,
            String searchQuery,
            Boolean isActive,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    ) {
        logger.info("[QuestionBankService] searchAndFilterBanks - ownerId={}, searchQuery='{}', isActive={}, createdFrom={}, createdTo={}, page={}, size={}",
                ownerId, searchQuery, isActive, createdFrom, createdTo, pageable.getPageNumber(), pageable.getPageSize());
        Specification<QuestionBank> spec =
                (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);

        if (searchQuery != null && !searchQuery.isBlank()) {
            String term = "%" + searchQuery.toLowerCase() + "%";
            Specification<QuestionBank> searchSpec =
                    (root, query, cb) ->
                            cb.or(
                                    cb.like(cb.lower(root.get("name")), term),
                                    cb.like(cb.lower(cb.coalesce(root.get("description"), cb.literal(""))), term));
            spec = spec.and(searchSpec);
        }
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }
        if (createdFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdDate"), createdFrom));
        }
        if (createdTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdDate"), createdTo));
        }

        Page<QuestionBank> result = questionBankRepository.findAll(spec, pageable);
        logger.info("[QuestionBankService] searchAndFilterBanks - Found {} banks (total: {})",
                result.getContent().size(), result.getTotalElements());
        return result;
    }

    @Transactional
    public QuestionBank updateBank(Long bankId, String ownerId, UpdateQuestionBankCommand command) {
        logger.info("[QuestionBankService] updateBank - bankId={}, ownerId={}", bankId, ownerId);
        QuestionBank bank = findOwnedBank(bankId, ownerId);
        
        if (command.name() != null) {
            String normalizedName = stringNormalizer.normalizeRequired(command.name());
            logger.debug("[QuestionBankService] updateBank - updating name: '{}' -> '{}'", bank.getName(), normalizedName);
            bank.setName(normalizedName);
        }
        if (command.description() != null) {
            String normalizedDescription = stringNormalizer.normalize(command.description());
            logger.debug("[QuestionBankService] updateBank - updating description: '{}' -> '{}'", 
                    bank.getDescription(), normalizedDescription);
            bank.setDescription(normalizedDescription);
        }
        if (command.isActive() != null) {
            logger.debug("[QuestionBankService] updateBank - updating isActive: {} -> {}", bank.getIsActive(), command.isActive());
            bank.setIsActive(command.isActive());
        }
        QuestionBank saved = questionBankRepository.save(bank);
        logger.info("[QuestionBankService] updateBank - Success: bank updated (id={})", saved.getBankId());
        return saved;
    }

    @Transactional
    public void deleteBank(Long bankId, String ownerId) {
        logger.info("[QuestionBankService] deleteBank - bankId={}, ownerId={}", bankId, ownerId);
        QuestionBank bank = findOwnedBank(bankId, ownerId);
        questionBankRepository.delete(bank);
        logger.info("[QuestionBankService] deleteBank - Success: bank deleted (id={}, name={})", bankId, bank.getName());
    }

    @Transactional(readOnly = true)
    public QuestionBank findOwnedBank(Long bankId, String ownerId) {
        logger.debug("[QuestionBankService] findOwnedBank - bankId={}, ownerId={}", bankId, ownerId);
        return questionBankRepository.findByBankIdAndOwnerId(bankId, ownerId)
                .orElseThrow(() -> {
                    logger.warn("[QuestionBankService] findOwnedBank - Bank not found or access denied: bankId={}, ownerId={}", bankId, ownerId);
                    return new NotFoundOrAccessDeniedException("Question bank not found or access denied");
                });
    }
}


