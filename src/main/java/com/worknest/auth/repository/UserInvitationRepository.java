package com.worknest.auth.repository;

import com.worknest.auth.domain.UserInvitation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

    Optional<UserInvitation> findByTokenHash(String tokenHash);

    Optional<UserInvitation> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);

    boolean existsByCompanyIdAndEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(
            UUID companyId,
            String email,
            Instant now
    );
}
