package ru.tms.user.database.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer{

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeDatabase() {
        logger.info("Initializing database tables...");
        createQuestionBanksTable();
        createTestTable();
        createQuestionsTable();
        createAnswerOptionsTable();
        logger.info("Database initialization completed");
    }

    private void createQuestionBanksTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS question_banks (
                    bank_id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    created_date DATE DEFAULT CURRENT_DATE,
                    is_active BOOLEAN DEFAULT TRUE
                )
                """;
            
            jdbcTemplate.execute(sql);
            logger.info("Table 'question_banks' created ");
        } catch (Exception e) {
            logger.error("Failed to create table 'question_banks': {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void createTestTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS tests (
                    test_id BIGSERIAL PRIMARY KEY,
                    bank_id BIGINT NOT NULL,
                    name VARCHAR(300) NOT NULL,
                    description TEXT,
                    difficulty VARCHAR(20) NOT NULL,
                    time_limit INT DEFAULT 0,
                    num_questions INT DEFAULT 0,
                    attempts INT DEFAULT 0,
                    status VARCHAR(20) DEFAULT 'DRAFT',
                    created_date DATE DEFAULT CURRENT_DATE,
                    published_date DATE,
                    CONSTRAINT fk_tests_question_banks FOREIGN KEY (bank_id) REFERENCES question_banks(bank_id) ON DELETE CASCADE
                )
                """;
            
            jdbcTemplate.execute(sql);
            logger.info("Table 'tests' created ");
        } catch (Exception e) {
            logger.error("Failed to create table 'tests': {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void createQuestionsTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS questions (
                    question_id BIGSERIAL PRIMARY KEY,
                    test_id BIGINT NOT NULL,
                    question_text TEXT NOT NULL,
                    question_type VARCHAR(30) NOT NULL,
                    difficulty VARCHAR(20) NOT NULL,
                    question_order INT DEFAULT 0,
                    CONSTRAINT fk_questions_tests FOREIGN KEY (test_id) REFERENCES tests(test_id) ON DELETE CASCADE
                )
                """;

            jdbcTemplate.execute(sql);
            logger.info("Table 'questions' created ");
        } catch (Exception e) {
            logger.error("Failed to create table 'questions': {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void createAnswerOptionsTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS answer_options (
                    option_id BIGSERIAL PRIMARY KEY,
                    question_id BIGINT NOT NULL,
                    option_text TEXT NOT NULL,
                    is_correct BOOLEAN DEFAULT FALSE,
                    display_order INT DEFAULT 0,
                    CONSTRAINT fk_answer_options_questions FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
                )
                """;

            jdbcTemplate.execute(sql);
            logger.info("Table 'answer_options' created ");
        } catch (Exception e) {
            logger.error("Failed to create table 'answer_options': {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

}