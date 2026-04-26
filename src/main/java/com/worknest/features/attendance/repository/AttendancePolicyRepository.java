package com.worknest.features.attendance.repository;

import com.worknest.domain.entities.AttendancePolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, UUID> {

    Optional<AttendancePolicy> findByCompanyIdAndSiteId(UUID companyId, UUID siteId);

    Optional<AttendancePolicy> findByCompanyIdAndSiteIsNull(UUID companyId);
}
