package ru.tms.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import ru.tms.user.entity.TestEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long>, JpaSpecificationExecutor<TestEntity> {
    Optional<TestEntity> findByTestIdAndOwnerId(Long testId, String ownerId);
    List<TestEntity> findAllByOwnerId(String ownerId);
    List<TestEntity> findAllByQuestionBank_BankIdAndOwnerId(Long bankId, String ownerId);

    @Query("""
            select distinct t
            from TestEntity t
            left join fetch t.questions q
            left join fetch q.answerOptions ao
            where t.testId = :testId and t.ownerId = :ownerId
            """)
    Optional<TestEntity> findByTestIdAndOwnerIdWithQuestions(@Param("testId") Long testId,
                                                            @Param("ownerId") String ownerId);
}














