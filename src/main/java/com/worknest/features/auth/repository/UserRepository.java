package com.worknest.features.auth.repository;

import com.worknest.domain.entities.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findAllByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    java.util.Optional<User> findById(UUID id);
}
