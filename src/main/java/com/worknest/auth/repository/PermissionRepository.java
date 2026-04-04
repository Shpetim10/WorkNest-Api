package com.worknest.auth.repository;

import com.worknest.auth.domain.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    Optional<Permission> findByResourceAndAction(String resource, String action);

    List<Permission> findAllByCodeIn(Collection<String> codes);
}
