package com.worknest.features.leave.repository;

import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.enums.LeaveStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Optional<LeaveRequest> findByIdAndCompanyId(UUID id, UUID companyId);

    List<LeaveRequest> findAllByCompanyIdAndEmployeeIdOrderByCreatedAtDesc(UUID companyId, UUID employeeId);

    Page<LeaveRequest> findAllByCompanyIdAndEmployeeIdOrderByCreatedAtDesc(UUID companyId, UUID employeeId, Pageable pageable);

    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.company.id = :companyId
              AND lr.employee.id = :employeeId
              AND lr.status IN :statuses
              AND lr.startDate <= :endDate
              AND lr.endDate >= :startDate
            """)
    List<LeaveRequest> findOverlapping(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveStatus> statuses
    );

    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.company.id = :companyId
              AND lr.employee.id = :employeeId
              AND lr.status = com.worknest.domain.enums.LeaveStatus.APPROVED
              AND lr.startDate <= :endDate
              AND lr.endDate >= :startDate
            ORDER BY lr.startDate ASC, lr.createdAt ASC
            """)
    List<LeaveRequest> findApprovedOverlappingPayrollPeriod(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.company.id = :companyId
              AND lr.employee.id = :employeeId
              AND lr.status = com.worknest.domain.enums.LeaveStatus.APPROVED
              AND lr.startDate <= :endDate
              AND lr.endDate >= :startDate
            ORDER BY lr.startDate ASC, lr.createdAt ASC
            """)
    List<LeaveRequest> findApprovedOverlappingRange(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
            SELECT lr FROM LeaveRequest lr
            JOIN lr.employee e
            JOIN e.user u
            WHERE lr.company.id = :companyId
              AND (:status IS NULL OR lr.status = :status)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY lr.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(lr) FROM LeaveRequest lr
            JOIN lr.employee e
            JOIN e.user u
            WHERE lr.company.id = :companyId
              AND (:status IS NULL OR lr.status = :status)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LeaveRequest> findAllForAdmin(
            @Param("companyId") UUID companyId,
            @Param("status") LeaveStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
