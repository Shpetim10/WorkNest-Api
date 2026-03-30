package com.worknest.auth.repository;

import com.worknest.auth.domain.User;
import com.worknest.auth.domain.UserStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByCompanyIdAndEmailIgnoreCase(UUID companyId, String email);

    Optional<User> findByCompanyIdAndEmailIgnoreCaseAndStatusIn(
            UUID companyId,
            String email,
            Collection<UserStatus> statuses
    );

    boolean existsByCompanyIdAndEmailIgnoreCase(UUID companyId, String email);
}
