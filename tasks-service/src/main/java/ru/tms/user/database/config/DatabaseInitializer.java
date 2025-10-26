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
        logger.info("Database initialization completed");
    }

    private void createQuestionBanksTable() {
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
        logger.info("Table 'question_banks' created or already exists");
    }

}