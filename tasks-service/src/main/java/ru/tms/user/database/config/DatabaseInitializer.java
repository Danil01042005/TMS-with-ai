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
        createUserFilesTable();
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
                    is_active BOOLEAN DEFAULT TRUE,
                    owner_id VARCHAR(128) NOT NULL
                )
                """;
            
            jdbcTemplate.execute(sql);
            jdbcTemplate.execute("""
                ALTER TABLE question_banks
                ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128)
            """);
            jdbcTemplate.execute("""
                UPDATE question_banks
                SET owner_id = COALESCE(owner_id, 'system')
            """);
            jdbcTemplate.execute("""
                ALTER TABLE question_banks
                ALTER COLUMN owner_id SET NOT NULL
            """);
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
                    owner_id VARCHAR(128) NOT NULL,
                    CONSTRAINT fk_tests_question_banks FOREIGN KEY (bank_id) REFERENCES question_banks(bank_id) ON DELETE CASCADE
                )
                """;
            
            jdbcTemplate.execute(sql);
            jdbcTemplate.execute("""
                ALTER TABLE tests
                ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128)
            """);
            jdbcTemplate.execute("""
                UPDATE tests t
                SET owner_id = qb.owner_id
                FROM question_banks qb
                WHERE t.bank_id = qb.bank_id
                  AND (t.owner_id IS NULL OR t.owner_id = '')
            """);
            jdbcTemplate.execute("""
                ALTER TABLE tests
                ALTER COLUMN owner_id SET NOT NULL
            """);
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

    private void createUserFilesTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS user_files (
                    file_id BIGSERIAL PRIMARY KEY,
                    owner_id VARCHAR(128) NOT NULL,
                    bank_id BIGINT,
                    test_id BIGINT,
                    object_key TEXT NOT NULL UNIQUE,
                    original_file_name TEXT NOT NULL,
                    content_type TEXT,
                    file_size_bytes BIGINT NOT NULL,
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_user_files_bank FOREIGN KEY (bank_id) REFERENCES question_banks(bank_id) ON DELETE CASCADE,
                    CONSTRAINT fk_user_files_test FOREIGN KEY (test_id) REFERENCES tests(test_id) ON DELETE CASCADE,
                    CONSTRAINT chk_user_files_one_target CHECK (
                        (bank_id IS NOT NULL AND test_id IS NULL) OR
                        (bank_id IS NULL AND test_id IS NOT NULL)
                    )
                )
                """;

            jdbcTemplate.execute(sql);
            logger.info("Table 'user_files' created");
        } catch (Exception e) {
            logger.error("Failed to create table 'user_files': {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

}