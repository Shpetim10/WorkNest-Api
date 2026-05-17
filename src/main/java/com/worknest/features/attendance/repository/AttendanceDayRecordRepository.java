package com.worknest.features.attendance.repository;

import com.worknest.domain.entities.AttendanceDayRecord;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceDayRecordRepository extends JpaRepository<AttendanceDayRecord, UUID> {

    Optional<AttendanceDayRecord> findByCompanyIdAndEmployeeIdAndWorkDate(UUID companyId, UUID employeeId, LocalDate workDate);

    Optional<AttendanceDayRecord> findByIdAndCompanyId(UUID id, UUID companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from AttendanceDayRecord r
            where r.company.id = :companyId
              and r.employee.id = :employeeId
              and r.workDate = :workDate
            """)
    Optional<AttendanceDayRecord> findWithLockByCompanyIdAndEmployeeIdAndWorkDate(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("workDate") LocalDate workDate
    );

    List<AttendanceDayRecord> findAllByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            UUID companyId,
            UUID employeeId,
            LocalDate from,
            LocalDate to
    );

    List<AttendanceDayRecord> findAllByCompanyIdAndWorkDateBetweenOrderByWorkDateAsc(UUID companyId, LocalDate from, LocalDate to);

    List<AttendanceDayRecord> findAllByCompanyIdAndWorkDate(UUID companyId, LocalDate workDate);

    @Modifying
    @Query("""
            UPDATE AttendanceDayRecord r
            SET r.payrollLocked = true
            WHERE r.company.id = :companyId
              AND r.employee.id = :employeeId
              AND r.workDate >= :from
              AND r.workDate <= :to
            """)
    void lockByCompanyIdAndEmployeeIdAndWorkDateBetween(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Modifying
    @Query("""
            UPDATE AttendanceDayRecord r
            SET r.payrollLocked = false
            WHERE r.company.id = :companyId
              AND r.employee.id = :employeeId
              AND r.workDate >= :from
              AND r.workDate <= :to
            """)
    void unlockByCompanyIdAndEmployeeIdAndWorkDateBetween(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            select r
            from AttendanceDayRecord r
            where r.company.id = :companyId
              and r.employee.id in :employeeIds
              and r.workDate = :workDate
            """)
    List<AttendanceDayRecord> findAllByCompanyIdAndEmployeeIdsAndWorkDate(
            @Param("companyId") UUID companyId,
            @Param("employeeIds") Collection<UUID> employeeIds,
            @Param("workDate") LocalDate workDate
    );
}
