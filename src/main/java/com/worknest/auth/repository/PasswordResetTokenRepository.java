package com.worknest.auth.repository;

import com.worknest.auth.domain.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);

    boolean existsByUserIdAndUsedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    boolean existsByUserIdAndUsedAtIsNull(UUID userId);

    java.util.List<PasswordResetToken> findAllByUserIdAndUsedAtIsNull(UUID userId);
}
