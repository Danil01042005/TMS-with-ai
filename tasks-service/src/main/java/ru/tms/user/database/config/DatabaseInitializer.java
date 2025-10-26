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
        logger.info("Database initialization completed");
    }

    private void createQuestionBanksTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS question_banks (
                    bank_id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
                    test_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    bank_id BIGINT NOT NULL,
                    name VARCHAR(300) NOT NULL,
                    description TEXT,
                    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL,
                    time_limit INT DEFAULT 0,
                    num_questions INT DEFAULT 0,
                    attempts INT DEFAULT 0,
                    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT',
                    created_date DATE DEFAULT CURRENT_DATE,
                    published_date DATE,
                    FOREIGN KEY (bank_id) REFERENCES question_banks(bank_id) ON DELETE CASCADE
                )
                """;
            
            jdbcTemplate.execute(sql);
            logger.info("Table 'tests' created ");
        } catch (Exception e) {
            logger.error("Failed to create table 'tests': {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

}