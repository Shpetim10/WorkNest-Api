package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.PayrollAdjustment;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, UUID> {

    List<PayrollAdjustment> findAllByCompanyIdAndEmployeeIdAndYearAndMonthOrderByCreatedAtAsc(
            UUID companyId, UUID employeeId, int year, int month);

    @Modifying
    @Query("UPDATE PayrollAdjustment pa SET pa.amount = pa.amount * :rate WHERE pa.company.id = :companyId")
    void convertAmountsByCompanyId(@Param("companyId") UUID companyId, @Param("rate") BigDecimal rate);
}
