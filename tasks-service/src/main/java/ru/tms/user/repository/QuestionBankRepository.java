package ru.tms.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tms.user.entity.QuestionBank;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
}
