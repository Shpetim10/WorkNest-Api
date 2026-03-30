package com.worknest.auth.repository;

import com.worknest.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken rt
               set rt.revokedAt = :revokedAt,
                   rt.revokedReason = :revokedReason
             where rt.id = :tokenId
               and rt.revokedAt is null
            """)
    int revokeById(
            @Param("tokenId") UUID tokenId,
            @Param("revokedAt") Instant revokedAt,
            @Param("revokedReason") String revokedReason
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken rt
               set rt.revokedAt = :revokedAt,
                   rt.revokedReason = :revokedReason
             where rt.user.id = :userId
               and rt.revokedAt is null
            """)
    int revokeAllActiveByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt,
            @Param("revokedReason") String revokedReason
    );
}
