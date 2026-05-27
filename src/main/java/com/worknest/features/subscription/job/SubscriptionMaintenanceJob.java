package com.worknest.features.subscription.job;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.features.company.repository.CompanyRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionMaintenanceJob {

    private final CompanyRepository companyRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processExpiredDeactivations() {
        Instant now = Instant.now();
        List<Company> due = companyRepository.findByDeletionScheduledAtBeforeAndStatusNot(now, CompanyStatus.DELETED);

        if (due.isEmpty()) {
            return;
        }

        log.info("Processing {} expired deactivation(s)", due.size());

        for (Company company : due) {
            try {
                company.setStatus(CompanyStatus.DELETED);
                company.setDeletedAt(now);
                company.setNipt(null);
                company.setNiptHash(null);
                company.setPhoneNumber(null);
                company.setSuspendedReason(null);
                companyRepository.save(company);
                log.info("Company {} marked as DELETED", company.getId());
            } catch (Exception e) {
                log.error("Failed to process deactivation for company {}: {}", company.getId(), e.getMessage());
            }
        }
    }
}
