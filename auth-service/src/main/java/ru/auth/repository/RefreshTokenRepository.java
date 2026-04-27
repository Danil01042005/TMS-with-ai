package ru.auth.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.auth.entity.RefreshToken;
import ru.auth.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    long deleteByUser(User user);
    long deleteByExpiresAtBefore(Instant cutoff);
    
    // Подсчет активных токенов пользователя (все токены в этой таблице активные)
    long countByUser(User user);
    
    // Поиск активных токенов пользователя, отсортированных по дате истечения (старые первые)
    List<RefreshToken> findByUserOrderByExpiresAtAsc(User user, Pageable pageable);
}




