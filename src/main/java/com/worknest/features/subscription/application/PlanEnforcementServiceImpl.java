package com.worknest.features.subscription.application;

import com.worknest.common.plan.PlanEnforcementService;
import com.worknest.domain.entities.PlanLimit;
import com.worknest.domain.entities.Subscription;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.subscription.exception.PlanLimitExceededException;
import com.worknest.features.subscription.exception.SubscriptionNotFoundException;
import com.worknest.features.subscription.repository.PlanLimitRepository;
import com.worknest.features.subscription.repository.SubscriptionRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanEnforcementServiceImpl implements PlanEnforcementService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanLimitRepository planLimitRepository;
    private final CompanyRepository companyRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public void assertCanAddEmployee(UUID companyId) {
        PlanLimit limit = resolveLimit(companyId);
        if (limit.getMaxEmployees() == null) return;
        long current = countEmployeesByRole(companyId, PlatformRole.EMPLOYEE);
        if (current >= limit.getMaxEmployees()) {
            throw new PlanLimitExceededException(
                    "Your " + limit.getPlan().name() + " plan allows a maximum of " + limit.getMaxEmployees()
                            + " employees. Upgrade your plan to add more.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanAddManager(UUID companyId) {
        PlanLimit limit = resolveLimit(companyId);
        if (limit.getMaxManagers() == null) return;
        long current = countEmployeesByRole(companyId, PlatformRole.STAFF);
        if (current >= limit.getMaxManagers()) {
            throw new PlanLimitExceededException(
                    "Your " + limit.getPlan().name() + " plan allows a maximum of " + limit.getMaxManagers()
                            + " managers. Upgrade your plan to add more.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanAddDepartment(UUID companyId) {
        PlanLimit limit = resolveLimit(companyId);
        if (limit.getMaxDepartments() == null) return;
        long current = (Long) entityManager
                .createQuery("SELECT COUNT(d) FROM Department d WHERE d.company.id = :cid")
                .setParameter("cid", companyId)
                .getSingleResult();
        if (current >= limit.getMaxDepartments()) {
            throw new PlanLimitExceededException(
                    "Your " + limit.getPlan().name() + " plan allows a maximum of " + limit.getMaxDepartments()
                            + " departments. Upgrade your plan to add more.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanAddLocation(UUID companyId) {
        PlanLimit limit = resolveLimit(companyId);
        if (limit.getMaxLocations() == null) return;
        long current = (Long) entityManager
                .createQuery("SELECT COUNT(s) FROM CompanySite s WHERE s.company.id = :cid")
                .setParameter("cid", companyId)
                .getSingleResult();
        if (current >= limit.getMaxLocations()) {
            throw new PlanLimitExceededException(
                    "Your " + limit.getPlan().name() + " plan allows a maximum of " + limit.getMaxLocations()
                            + " locations. Upgrade your plan to add more.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertPayrollEnabled(UUID companyId) {
        PlanLimit limit = resolveLimit(companyId);
        if (!Boolean.TRUE.equals(limit.getPayrollEnabled())) {
            throw new PlanLimitExceededException(
                    "Payroll is not available on the " + limit.getPlan().name()
                            + " plan. Upgrade to GROWTH or PROFESSIONAL to enable payroll.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertAuditLogsEnabled(UUID companyId) {
        PlanLimit limit = resolveLimit(companyId);
        if (!Boolean.TRUE.equals(limit.getAuditLogsEnabled())) {
            throw new PlanLimitExceededException(
                    "Audit logs are not available on the " + limit.getPlan().name()
                            + " plan. Upgrade to PROFESSIONAL to enable audit logs.");
        }
    }

    private long countEmployeesByRole(UUID companyId, PlatformRole role) {
        return (Long) entityManager
                .createQuery("SELECT COUNT(e) FROM Employee e WHERE e.company.id = :cid AND e.employmentTypeRole = :role")
                .setParameter("cid", companyId)
                .setParameter("role", role)
                .getSingleResult();
    }

    private PlanLimit resolveLimit(UUID companyId) {
        Optional<Subscription> sub = subscriptionRepository
                .findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(
                        companyId,
                        List.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE));

        SubscriptionPlan plan;
        if (sub.isPresent()) {
            plan = sub.get().getPlan();
        } else {
            // Company registered without a payment method — no Subscription record yet.
            // Fall back to the plan recorded on the Company itself.
            plan = companyRepository.findById(companyId)
                    .map(c -> c.getSubscriptionPlan() != null ? c.getSubscriptionPlan() : SubscriptionPlan.FOUNDATION)
                    .orElseThrow(SubscriptionNotFoundException::new);
        }

        return planLimitRepository.findByPlan(plan)
                .orElseThrow(() -> new IllegalStateException("No plan limits configured for plan: " + plan));
    }
}
