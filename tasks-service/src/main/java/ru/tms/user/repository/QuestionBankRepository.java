package ru.tms.user.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ru.tms.user.entity.QuestionBank;

import java.time.LocalDate;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long>, JpaSpecificationExecutor<QuestionBank> {
    java.util.Optional<QuestionBank> findByBankIdAndOwnerId(Long bankId, String ownerId);

    Page<QuestionBank> findByOwnerId(String ownerId, Pageable pageable);

    @Query("""
        select qb
        from QuestionBank qb
        where qb.ownerId = :ownerId
          and (
               :searchQuery is null or :searchQuery = '' or
               lower(qb.name) like lower(concat('%', :searchQuery, '%')) or
               lower(qb.description) like lower(concat('%', :searchQuery, '%'))
          )
          and (:isActive is null or qb.isActive = :isActive)
          and (:createdFrom is null or qb.createdDate >= :createdFrom)
          and (:createdTo is null or qb.createdDate <= :createdTo)
        """)
    Page<QuestionBank> searchAndFilter(
            String ownerId,
            String searchQuery,
            Boolean isActive,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    );
}
