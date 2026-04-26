package com.worknest.features.attendance.repository;

import com.worknest.domain.entities.AttendanceQrTerminal;
import com.worknest.domain.enums.AttendanceQrTerminalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceQrTerminalRepository extends JpaRepository<AttendanceQrTerminal, UUID> {

    List<AttendanceQrTerminal> findAllBySiteIdOrderByCreatedAtAsc(UUID siteId);

    Optional<AttendanceQrTerminal> findFirstBySiteIdAndStatusOrderByCreatedAtAsc(UUID siteId, AttendanceQrTerminalStatus status);

    Optional<AttendanceQrTerminal> findByIdAndCompanyId(UUID id, UUID companyId);
}
