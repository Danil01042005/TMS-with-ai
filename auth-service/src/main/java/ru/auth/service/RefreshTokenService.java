package ru.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import ru.auth.entity.RefreshToken;
import ru.auth.entity.RevokedTokenBlacklist;
import ru.auth.entity.User;
import ru.auth.repository.RefreshTokenRepository;
import ru.auth.repository.RevokedTokenBlacklistRepository;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenBlacklistRepository revokedTokenBlacklistRepository;

    @Value("${auth.refresh.expires:P7D}")
    private Duration refreshTokenTtl;

    @Value("${auth.refresh.max-tokens-per-user:5}")
    private int maxTokensPerUser;

    private static final SecureRandom random = new SecureRandom();

    public String generateTokenString() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }

    public RefreshToken issue(User user) {
        // Проверяем количество активных токенов пользователя
        // В основной таблице хранятся только активные токены
        long activeCount = refreshTokenRepository.countByUser(user);
        
        // Если превышен лимит, отзываем самые старые токены
        if (activeCount >= maxTokensPerUser) {
            int tokensToRevoke = (int) (activeCount - maxTokensPerUser + 1);
            log.debug("User {} has {} active tokens (limit: {}), revoking {} oldest tokens", 
                    user.getUsername(), activeCount, maxTokensPerUser, tokensToRevoke);
            
            // Находим самые старые активные токены (отсортированы по expiresAt)
            List<RefreshToken> oldTokens = refreshTokenRepository
                    .findByUserOrderByExpiresAtAsc(
                            user, 
                            PageRequest.of(0, tokensToRevoke));
            
            // Отзываем самые старые
            for (RefreshToken oldToken : oldTokens) {
                revoke(oldToken);
                log.debug("Revoked old token for user {} (expires at: {})", 
                        user.getUsername(), oldToken.getExpiresAt());
            }
        }
        
        // Создаем новый токен (все токены в основной таблице активные)
        String tokenValue = generateTokenString();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(tokenValue))
                .expiresAt(Instant.now().plus(refreshTokenTtl))
                .build();
        RefreshToken saved = refreshTokenRepository.save(token);
        saved.setToken(tokenValue);
        return saved;
    }

    public RefreshToken validateActive(String tokenValue) {
        String hash = sha256(tokenValue);
        
        // 1. Сначала проверяем blacklist (быстрая проверка по хешу)
        if (revokedTokenBlacklistRepository.existsByTokenHash(hash)) {
            // ✅ ОБНАРУЖЕНА ПОПЫТКА КОМПРОМЕТАЦИИ!
            log.error("SECURITY ALERT: Attempt to use revoked refresh token. Hash: {}", hash);
            throw new IllegalStateException("refresh token revoked");
        }
        
        // 2. Ищем в активных токенах (основная таблица содержит только активные)
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("refresh token not found"));
        
        // 3. Проверяем срок действия
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("refresh token expired");
        }
        
        token.setToken(tokenValue);
        return token;
    }

    public void revoke(RefreshToken token) {
        // 1. Добавляем в blacklist (для обнаружения попыток компрометации)
        RevokedTokenBlacklist blacklistEntry = RevokedTokenBlacklist.builder()
                .tokenHash(token.getTokenHash())
                .revokedAt(Instant.now())
                .userId(token.getUser().getId())
                .build();
        revokedTokenBlacklistRepository.save(blacklistEntry);
        
        // 2. Удаляем из активных токенов (основная таблица содержит только активные)
        refreshTokenRepository.delete(token);
        
        log.info("Revoked refresh token for user: {} (token ID: {})", 
                 token.getUser().getUsername(), token.getId());
    }

    public RefreshToken rotateByValue(String tokenValue) {
        // validateActive проверяет blacklist и срок действия
        RefreshToken token = validateActive(tokenValue);
        
        // Отзываем старый токен (добавляет в blacklist и удаляет из активных)
        revoke(token);
        
        // Выдаем новый токен
        return issue(token.getUser());
    }

    public long purgeExpired() {
        return refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    @Scheduled(cron = "0 0 2 * * *")  // Каждый день в 2:00 ночи (оптимизация частоты)
    public void purgeExpiredJob() {
        log.info("Starting expired tokens purge job...");
        long deleted = purgeExpired();
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        } else {
            log.debug("No expired refresh tokens to purge");
        }
    }

    /**
     * Удаляет старые записи из blacklist (старше 1 часа).
     * Blacklist хранит отозванные токены 1 час для обнаружения попыток компрометации.
     * Большинство атак происходят сразу, поэтому 1 часа достаточно.
     */
    @Scheduled(cron = "0 0 * * * *")  // Каждый час
    public void purgeBlacklist() {
        log.debug("Starting revoked tokens blacklist purge job...");
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        long deleted = revokedTokenBlacklistRepository.deleteByRevokedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} old entries from revoked tokens blacklist (older than 1 hour)", deleted);
        } else {
            log.debug("No old entries in blacklist to purge");
        }
    }
}