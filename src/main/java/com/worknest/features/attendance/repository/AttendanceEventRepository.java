package com.worknest.features.attendance.repository;

import com.worknest.domain.entities.AttendanceEvent;
import com.worknest.domain.enums.AttendanceEventStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, UUID> {

    Optional<AttendanceEvent> findByCompanyIdAndClientRequestId(UUID companyId, String clientRequestId);

    Optional<AttendanceEvent> findByIdAndCompanyId(UUID id, UUID companyId);

    List<AttendanceEvent> findAllByCompanyIdAndEmployeeIdAndWorkDateAndEventStatusInOrderByServerRecordedAtAsc(
            UUID companyId,
            UUID employeeId,
            LocalDate workDate,
            List<AttendanceEventStatus> statuses
    );

    Optional<AttendanceEvent> findTopByCompanyIdAndEmployeeIdAndWorkDateAndEventStatusInOrderByServerRecordedAtDesc(
            UUID companyId,
            UUID employeeId,
            LocalDate workDate,
            List<AttendanceEventStatus> statuses
    );

    List<AttendanceEvent> findAllByCompanyIdAndEmployeeIdAndWorkDateOrderByServerRecordedAtAsc(
            UUID companyId,
            UUID employeeId,
            LocalDate workDate
    );
}
