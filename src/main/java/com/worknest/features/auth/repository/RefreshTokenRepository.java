package com.worknest.features.auth.repository;

import com.worknest.domain.entities.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("""
            select rt from RefreshToken rt
            left join fetch rt.user
            left join fetch rt.activeRoleAssignment ra
            left join fetch ra.company
            where rt.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashWithAuditing(@Param("tokenHash") String tokenHash);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(UUID userId);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from RefreshToken rt
             where rt.expiresAt < :now
                or rt.revokedAt is not null
            """)
    void deleteByExpiresAtBeforeOrRevokedAtIsNotNull(@Param("now") Instant now);
}
