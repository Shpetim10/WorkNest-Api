package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.PayrollResult;
import com.worknest.domain.enums.PayrollStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollResultRepository extends JpaRepository<PayrollResult, UUID> {

    Optional<PayrollResult> findByCompanyIdAndEmployeeIdAndYearAndMonth(
            UUID companyId, UUID employeeId, int year, int month);

    boolean existsByCompanyIdAndEmployeeIdAndYearAndMonthAndStatusIn(
            UUID companyId, UUID employeeId, int year, int month, Collection<PayrollStatus> statuses);

    List<PayrollResult> findAllByCompanyId(UUID companyId);

    List<PayrollResult> findAllByCompanyIdAndEmployeeIdOrderByYearDescMonthDesc(UUID companyId, UUID employeeId);
}
