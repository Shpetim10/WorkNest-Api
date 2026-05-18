package com.worknest.features.superAdmin.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.audit.domain.PlatformEvent;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.dashboard.application.DashboardDateRange;
import com.worknest.features.superAdmin.dto.SuperAdminDashboardResponse;
import com.worknest.security.SuperAdminSecurity;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminDashboardServiceImpl implements SuperAdminDashboardService {

    private static final DateTimeFormatter ACTIVITY_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneOffset.UTC);
    private static final String REAL_COMPANIES = "c.deletedAt IS NULL AND c.slug != 'worknest-platform'";
    private static final String COUNT_ALL_JPQL = "SELECT COUNT(c) FROM Company c WHERE " + REAL_COMPANIES;
    private static final String COUNT_BY_STATUS_JPQL = "SELECT COUNT(c) FROM Company c WHERE c.status = :s AND " + REAL_COMPANIES;

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final SuperAdminSecurity superAdminSecurity;

    @Override
    public SuperAdminDashboardResponse getDashboard(int year, String period, String startDate, String endDate, String section) {
        Instant now = Instant.now();
        DashboardSection dashboardSection = DashboardSection.from(section);
        Optional<DashboardDateRange> customRange = DashboardDateRange.parseOptional(startDate, endDate);

        long total = countAllCompanies();
        long active = countCompaniesByStatus(CompanyStatus.ACTIVE);
        long suspended = countCompaniesByStatus(CompanyStatus.SUSPENDED);
        long expiringSoon = countExpiringSoon(now);

        List<Company> allLive = entityManager.createQuery(
                        "SELECT c FROM Company c WHERE " + REAL_COMPANIES, Company.class)
                .getResultList();

        List<Company> periodFiltered = filterByPeriod(allLive, period, now);
        List<Company> customFiltered = customRange
                .map(range -> filterByDateRange(allLive, range))
                .orElse(periodFiltered);
        List<Company> planCompanies = dashboardSection.filtersQuickStatsOnly() ? periodFiltered : customFiltered;
        List<Company> quickStatCompanies = dashboardSection.filtersSubscriptionPlansOnly() ? periodFiltered : customFiltered;

        List<SuperAdminDashboardResponse.RegistrationPoint> registrations = buildRegistrations(allLive, year);
        List<SuperAdminDashboardResponse.SubscriptionPlanBreakdown> plans = buildPlanBreakdown(planCompanies);
        List<SuperAdminDashboardResponse.ActivityItem> recentActivity = buildRecentActivity();
        List<SuperAdminDashboardResponse.QuickStat> quickStats = buildQuickStats(quickStatCompanies);

        SuperAdminDashboardResponse.Header header = buildHeader();
        SuperAdminDashboardResponse.Kpis kpis = new SuperAdminDashboardResponse.Kpis(total, active, suspended, expiringSoon);

        return new SuperAdminDashboardResponse(header, kpis, registrations, plans, recentActivity, quickStats);
    }

    private List<Company> filterByPeriod(List<Company> companies, String period, Instant now) {
        if (period == null || period.isBlank()) return companies;
        ZonedDateTime nowUTC = ZonedDateTime.ofInstant(now, ZoneOffset.UTC);
        Instant from = switch (period) {
            case "this-week" -> nowUTC.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "this-month" -> nowUTC.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "last-month" -> nowUTC.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "last-3-months" -> nowUTC.minusMonths(3).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "last-6-months" -> nowUTC.minusMonths(6).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "this-year" -> nowUTC.withDayOfYear(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            default -> null;
        };
        Instant to = "last-month".equals(period)
                ? nowUTC.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                : now;
        if (from == null) return companies;
        return companies.stream()
                .filter(c -> !c.getCreatedAt().isBefore(from) && !c.getCreatedAt().isAfter(to))
                .toList();
    }

    private List<Company> filterByDateRange(List<Company> companies, DashboardDateRange range) {
        Instant from = range.startDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = range.endDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return companies.stream()
                .filter(c -> c.getCreatedAt() != null)
                .filter(c -> !c.getCreatedAt().isBefore(from) && c.getCreatedAt().isBefore(toExclusive))
                .toList();
    }

    private SuperAdminDashboardResponse.Header buildHeader() {
        String displayName = superAdminSecurity.currentPrincipal()
                .flatMap(p -> userRepository.findById(p.userId()))
                .map(this::resolveDisplayName)
                .orElse("Super Admin");
        return new SuperAdminDashboardResponse.Header(displayName, null, null);
    }

    private long countExpiringSoon(Instant now) {
        Instant soon = now.plus(30, ChronoUnit.DAYS);
        return entityManager.createQuery(
                        "SELECT COUNT(c) FROM Company c WHERE " + REAL_COMPANIES
                                + " AND c.subscriptionStatus = :trial"
                                + " AND c.trialEndsAt IS NOT NULL"
                                + " AND c.trialEndsAt >= :now AND c.trialEndsAt <= :soon",
                        Long.class)
                .setParameter("trial", SubscriptionStatus.TRIAL)
                .setParameter("now", now)
                .setParameter("soon", soon)
                .getSingleResult();
    }

    private List<SuperAdminDashboardResponse.RegistrationPoint> buildRegistrations(List<Company> companies, int year) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        Map<Integer, Long> byMonth = companies.stream()
                .filter(c -> ZonedDateTime.ofInstant(c.getCreatedAt(), ZoneOffset.UTC).getYear() == year)
                .collect(Collectors.groupingBy(
                        c -> ZonedDateTime.ofInstant(c.getCreatedAt(), ZoneOffset.UTC).getMonthValue(),
                        Collectors.counting()
                ));

        long max = byMonth.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        List<SuperAdminDashboardResponse.RegistrationPoint> result = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            long count = byMonth.getOrDefault(i, 0L);
            double pct = max > 0 ? (count * 100.0 / max) : 0.0;
            result.add(new SuperAdminDashboardResponse.RegistrationPoint(months[i - 1], count, pct));
        }

        return result;
    }

    private List<SuperAdminDashboardResponse.SubscriptionPlanBreakdown> buildPlanBreakdown(List<Company> companies) {
        Map<SubscriptionPlan, Long> byPlan = companies.stream()
                .collect(Collectors.groupingBy(Company::getSubscriptionPlan, Collectors.counting()));

        long total = companies.size();
        List<SuperAdminDashboardResponse.SubscriptionPlanBreakdown> result = new ArrayList<>();

        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            long count = byPlan.getOrDefault(plan, 0L);
            double pct = total > 0 ? (count * 100.0 / total) : 0.0;
            result.add(new SuperAdminDashboardResponse.SubscriptionPlanBreakdown(
                    plan.name().toLowerCase(), plan.name(), count, pct));
        }

        return result;
    }

    private List<SuperAdminDashboardResponse.ActivityItem> buildRecentActivity() {
        List<PlatformEvent> events = entityManager.createQuery(
                        "SELECT p FROM PlatformEvent p ORDER BY p.createdAt DESC",
                        PlatformEvent.class)
                .setMaxResults(10)
                .getResultList();

        Set<UUID> actorIds = events.stream()
                .map(PlatformEvent::getActorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> actorNames = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, this::resolveDisplayName));

        return events.stream()
                .map(e -> new SuperAdminDashboardResponse.ActivityItem(
                        String.valueOf(e.getId()),
                        e.getActorUserId() != null ? actorNames.getOrDefault(e.getActorUserId(), "System") : "System",
                        e.getDescription() != null ? e.getDescription() : e.getEventType(),
                        ACTIVITY_FMT.format(e.getCreatedAt())
                ))
                .toList();
    }

    private List<SuperAdminDashboardResponse.QuickStat> buildQuickStats(List<Company> companies) {
        long total = companies.size();
        long active = companies.stream().filter(c -> c.getStatus() == CompanyStatus.ACTIVE).count();
        long suspended = companies.stream().filter(c -> c.getStatus() == CompanyStatus.SUSPENDED).count();
        long trial = companies.stream().filter(c -> c.getSubscriptionStatus() == SubscriptionStatus.TRIAL).count();

        double activePct = total > 0 ? (active * 100.0 / total) : 0.0;
        double trialPct = total > 0 ? (trial * 100.0 / total) : 0.0;
        double suspendedPct = total > 0 ? (suspended * 100.0 / total) : 0.0;

        return List.of(
                new SuperAdminDashboardResponse.QuickStat("active", "Active", fmt(activePct), activePct),
                new SuperAdminDashboardResponse.QuickStat("trial", "Trial", fmt(trialPct), trialPct),
                new SuperAdminDashboardResponse.QuickStat("suspended", "Suspended", fmt(suspendedPct), suspendedPct)
        );
    }

    private long countAllCompanies() {
        return entityManager.createQuery(COUNT_ALL_JPQL, Long.class).getSingleResult();
    }

    private long countCompaniesByStatus(CompanyStatus status) {
        return entityManager.createQuery(COUNT_BY_STATUS_JPQL, Long.class)
                .setParameter("s", status)
                .getSingleResult();
    }

    private String resolveDisplayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private String fmt(double pct) {
        return Math.round(pct) + "%";
    }

    private enum DashboardSection {
        ALL,
        SUBSCRIPTION_PLANS,
        QUICK_STATS;

        private static DashboardSection from(String section) {
            if (section == null || section.isBlank()) {
                return ALL;
            }
            return switch (section.trim()) {
                case "subscriptionPlans" -> SUBSCRIPTION_PLANS;
                case "quickStats" -> QUICK_STATS;
                default -> throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_DASHBOARD_SECTION",
                        "section must be either subscriptionPlans or quickStats."
                );
            };
        }

        private boolean filtersSubscriptionPlansOnly() {
            return this == SUBSCRIPTION_PLANS;
        }

        private boolean filtersQuickStatsOnly() {
            return this == QUICK_STATS;
        }
    }
}
