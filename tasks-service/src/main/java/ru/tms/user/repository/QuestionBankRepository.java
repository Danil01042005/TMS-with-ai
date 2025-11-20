package ru.tms.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tms.user.entity.QuestionBank;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    java.util.Optional<QuestionBank> findByBankIdAndOwnerId(Long bankId, String ownerId);
    Page<QuestionBank> findByOwnerIdOrderByBankIdDesc(String ownerId, Pageable pageable);
    
    Page<QuestionBank> findByOwnerIdAndNameContainingIgnoreCaseOrOwnerIdAndDescriptionContainingIgnoreCase(
            String ownerId1, String nameQuery, String ownerId2, String descriptionQuery, Pageable pageable);
}
