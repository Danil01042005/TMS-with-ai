package ru.tms.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.tms.user.entity.QuestionEntity;

import java.util.List;
        
@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    @Query("""
            select distinct q
            from QuestionEntity q
            join q.test t
            left join fetch q.answerOptions ao
            where t.testId = :testId and t.ownerId = :ownerId
            """)
    List<QuestionEntity> findAllByTestIdAndOwnerIdWithOptions(@Param("testId") Long testId,
                                                             @Param("ownerId") String ownerId);
}


