package com.worknest.auth.repository;

import com.worknest.auth.domain.UserInvitation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

    Optional<UserInvitation> findByTokenHash(String tokenHash);

    Optional<UserInvitation> findByTokenHashAndUsedAtIsNull(String tokenHash);

    Optional<UserInvitation> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);

    boolean existsByCompanyIdAndEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(
            UUID companyId,
            String email,
            Instant now
    );

    Page<UserInvitation> findAllByCompanyId(UUID companyId, Pageable pageable);

    List<UserInvitation> findAllByCompanyIdAndUsedAtIsNullAndExpiresAtAfter(UUID companyId, Instant now);

    List<UserInvitation> findAllByCompanyIdAndUsedAtIsNull(UUID companyId);

    List<UserInvitation> findAllByEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(String email, Instant now);

    List<UserInvitation> findAllByEmailIgnoreCaseAndUsedAtIsNull(String email);

    Optional<UserInvitation> findByCompanyIdAndEmailIgnoreCase(UUID companyId, String email);

    Optional<UserInvitation> findByCompanyIdAndEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(
            UUID companyId,
            String email,
            Instant now
    );

    @Modifying
    @Query("UPDATE UserInvitation ui SET ui.usedAt = :usedAt WHERE ui.id = :id")
    void markAsUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("DELETE FROM UserInvitation ui WHERE ui.expiresAt < :now AND ui.usedAt IS NULL")
    void deleteExpired(@Param("now") Instant now);
}
