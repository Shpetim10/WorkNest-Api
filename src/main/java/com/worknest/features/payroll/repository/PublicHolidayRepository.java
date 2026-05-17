package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.PublicHoliday;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

    List<PublicHoliday> findAllByCompanyIdAndHolidayDateBetween(
            UUID companyId, LocalDate from, LocalDate to);

    @Query("""
            SELECT h FROM PublicHoliday h
            WHERE h.company.id = :companyId
              AND (
                    (h.recurring = false AND h.holidayDate BETWEEN :from AND :to)
                    OR
                    (h.recurring = true
                     AND FUNCTION('date_part', 'month', h.holidayDate) = FUNCTION('date_part', 'month', CAST(:from AS date))
                       OR h.recurring = true AND h.holidayDate BETWEEN :yearStart AND :to)
                  )
            """)
    List<PublicHoliday> findAllByCompanyId(@Param("companyId") UUID companyId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           @Param("yearStart") LocalDate yearStart);

    List<PublicHoliday> findAllByCompanyId(UUID companyId);

    Optional<PublicHoliday> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndHolidayDate(UUID companyId, LocalDate date);
}
