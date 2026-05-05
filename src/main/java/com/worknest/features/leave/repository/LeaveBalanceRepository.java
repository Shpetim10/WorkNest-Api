package com.worknest.features.leave.repository;

import com.worknest.domain.entities.LeaveBalance;
import com.worknest.domain.enums.LeaveType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance> findByCompanyIdAndEmployeeIdAndYearAndLeaveType(
            UUID companyId, UUID employeeId, int year, LeaveType leaveType);

    List<LeaveBalance> findAllByCompanyIdAndEmployeeIdAndYear(UUID companyId, UUID employeeId, int year);
}