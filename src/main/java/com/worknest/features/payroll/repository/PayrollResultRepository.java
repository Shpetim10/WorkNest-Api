package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.PayrollResult;
import com.worknest.domain.enums.PayrollStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollResultRepository extends JpaRepository<PayrollResult, UUID> {

    Optional<PayrollResult> findByCompanyIdAndEmployeeIdAndYearAndMonth(
            UUID companyId, UUID employeeId, int year, int month);

    boolean existsByCompanyIdAndEmployeeIdAndYearAndMonthAndStatusIn(
            UUID companyId, UUID employeeId, int year, int month, Collection<PayrollStatus> statuses);

    @Modifying
    @Query("""
            UPDATE PayrollResult pr
            SET pr.basePay        = pr.basePay        * :rate,
                pr.grossEarnings  = pr.grossEarnings  * :rate,
                pr.totalDeductions= pr.totalDeductions* :rate,
                pr.netPay         = pr.netPay         * :rate
            WHERE pr.company.id = :companyId
            """)
    void convertAmountsByCompanyId(@Param("companyId") UUID companyId, @Param("rate") BigDecimal rate);
}
