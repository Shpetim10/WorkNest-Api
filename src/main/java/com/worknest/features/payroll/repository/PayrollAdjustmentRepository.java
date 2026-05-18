package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.PayrollAdjustment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, UUID> {

    List<PayrollAdjustment> findAllByCompanyIdAndEmployeeIdAndYearAndMonthOrderByCreatedAtAsc(
            UUID companyId, UUID employeeId, int year, int month);

    List<PayrollAdjustment> findAllByCompanyId(UUID companyId);
  
    void deleteAllByCompanyIdAndEmployeeIdAndYearAndMonth(
            UUID companyId, UUID employeeId, int year, int month);
}
