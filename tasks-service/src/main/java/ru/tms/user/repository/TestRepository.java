package ru.tms.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tms.user.entity.TestEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {
    Optional<TestEntity> findByTestIdAndOwnerId(Long testId, String ownerId);
    List<TestEntity> findAllByOwnerId(String ownerId);
    List<TestEntity> findAllByQuestionBank_BankIdAndOwnerId(Long bankId, String ownerId);
}




