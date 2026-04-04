package com.worknest.features.auth.repository;

import com.worknest.domain.entities.User;
import com.worknest.domain.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByCompany_SlugIgnoreCaseAndEmailIgnoreCase(String companySlug, String email);

    Optional<User> findByCompany_SlugIgnoreCaseAndUsernameIgnoreCase(String companySlug, String username);

    Optional<User> findByCompany_SlugIgnoreCaseAndEmailIgnoreCaseAndStatusIn(
            String companySlug,
            String email,
            Collection<UserStatus> statuses
    );

    Optional<User> findByCompany_SlugIgnoreCaseAndUsernameIgnoreCaseAndStatusIn(
            String companySlug,
            String username,
            Collection<UserStatus> statuses
    );

    Optional<User> findByCompanyIdAndEmailIgnoreCase(UUID companyId, String email);

    Optional<User> findByCompanyIdAndUsernameIgnoreCase(UUID companyId, String username);

    Optional<User> findByCompanyIdAndEmailIgnoreCaseAndStatusIn(
            UUID companyId,
            String email,
            Collection<UserStatus> statuses
    );

    Optional<User> findByCompanyIdAndUsernameIgnoreCaseAndStatusIn(
            UUID companyId,
            String username,
            Collection<UserStatus> statuses
    );

    boolean existsByCompanyIdAndEmailIgnoreCase(UUID companyId, String email);

    boolean existsByCompanyIdAndUsernameIgnoreCase(UUID companyId, String username);

    boolean existsByCompany_SlugIgnoreCaseAndEmailIgnoreCase(String companySlug, String email);

    boolean existsByCompany_SlugIgnoreCaseAndUsernameIgnoreCase(String companySlug, String username);

    List<User> findAllByCompanyIdAndIdIn(UUID companyId, Collection<UUID> userIds);
}
