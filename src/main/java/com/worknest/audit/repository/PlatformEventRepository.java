package com.worknest.audit.repository;

import com.worknest.audit.domain.PlatformEvent;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformEventRepository extends JpaRepository<PlatformEvent, Long> {

    Page<PlatformEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PlatformEvent> findAllByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);

    Page<PlatformEvent> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);
}
