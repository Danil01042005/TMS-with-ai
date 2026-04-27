package ru.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.auth.entity.RevokedTokenBlacklist;

import java.time.Instant;

/**
 * Репозиторий для работы с blacklist отозванных токенов.
 */
@Repository
public interface RevokedTokenBlacklistRepository extends JpaRepository<RevokedTokenBlacklist, String> {
    
    /**
     * Проверяет, существует ли токен в blacklist.
     */
    boolean existsByTokenHash(String tokenHash);
    
    /**
     * Удаляет записи, которые были отозваны раньше указанной даты.
     * Используется для автоматической очистки старых записей.
     */
    long deleteByRevokedAtBefore(Instant cutoff);
}


