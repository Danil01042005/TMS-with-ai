package ru.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Легковесная таблица для хранения отозванных токенов (только хеш).
 * Используется для обнаружения попыток компрометации.
 * Записи автоматически удаляются через 1 час после отзыва.
 */
@Entity
@Table(name = "revoked_tokens_blacklist", indexes = {
        @Index(name = "idx_revoked_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_revoked_at", columnList = "revoked_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedTokenBlacklist {

    @Id
    @Column(name = "token_hash", length = 128)
    private String tokenHash;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}


