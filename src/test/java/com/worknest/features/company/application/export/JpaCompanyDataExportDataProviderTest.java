package com.worknest.features.company.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.PayrollAdjustment;
import com.worknest.domain.entities.PayrollResult;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.domain.enums.PlatformRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaCompanyDataExportDataProviderTest {

    private static final List<String> EXPECTED_PATHS = List.of(
            "employees/employee-list.xlsx",
            "employees/staff-list.xlsx",
            "employees/assign-employees.xlsx",
            "attendance/attendance.xlsx",
            "leave/leave.xlsx",
            "payroll/payroll.xlsx",
            "locations/locations.xlsx",
            "departments/departments.xlsx",
            "announcements/announcements.xlsx",
            "audit-log/audit-log.xlsx"
    );

    @Test
    void loadCompanyDataRunsEveryModuleQueryForTheLoggedInCompany() {
        List<QueryCall> issuedQueries = new ArrayList<>();
        UUID companyId = UUID.randomUUID();
        JpaCompanyDataExportDataProvider provider = new JpaCompanyDataExportDataProvider(
                entityManagerProxy(issuedQueries, Map.of()),
                new ExportLocalizationService()
        );

        List<ExportWorkbookData> workbooks = provider.loadCompanyData(companyId, "en");

        assertThat(workbooks).extracting(ExportWorkbookData::path).containsExactlyElementsOf(EXPECTED_PATHS);
        assertThat(issuedQueries).hasSize(EXPECTED_PATHS.size() + 2);
        assertThat(issuedQueries).allSatisfy(query ->
                assertThat(query.parameters()).containsEntry("companyId", companyId)
        );
    }

    @Test
    void loadCompanyDataAddsRowsForEveryDatabaseBackedModuleWorkbook() {
        UUID companyId = UUID.randomUUID();
        PayrollFixture payrollFixture = payrollFixture(companyId);
        EntityManager entityManager = entityManagerProxy(new ArrayList<>(), Map.of(
                "export:employee-list", List.<Object[]>of(new Object[]{
                        "Ada Lovelace", "EMPLOYEE", "FULL_TIME", "ada@example.test",
                        "Engineering", "HQ", "Developer", "ACTIVE"
                }),
                "export:staff-list", List.<Object[]>of(new Object[]{
                        "Grace Hopper", "grace@example.test", "Engineering", "HQ",
                        "Manager", 2L, "ACTIVE"
                }),
                "export:assign-employees", List.<Object[]>of(new Object[]{
                        "Grace Hopper", "Manager", "Engineering", 2L
                }),
                "export:attendance", List.<Object[]>of(new Object[]{
                        "Ada Lovelace", "HQ", "Engineering", "NONE", null, null, "ABSENT", 0, false
                }),
                "export:leave", List.<Object[]>of(new Object[]{
                        "Ada Lovelace", "HQ", "Engineering", "VACATION",
                        Date.valueOf("2026-05-01"), Date.valueOf("2026-05-02"),
                        BigDecimal.valueOf(2), "PENDING"
                }),
                "export:locations", List.<Object[]>of(new Object[]{
                        "HQ", "HQ", "HQ", "AL", "ACTIVE", Timestamp.from(Instant.parse("2026-05-01T10:00:00Z"))
                }),
                "export:departments", List.<Object[]>of(new Object[]{
                        "Engineering", "ACTIVE", "Builds things", 2L, Timestamp.from(Instant.parse("2026-05-01T10:00:00Z"))
                }),
                "export:announcements", List.<Object[]>of(new Object[]{
                        "Launch", "ALL_EMPLOYEES", "IMPORTANT", "Ada Lovelace",
                        Timestamp.from(Instant.parse("2026-05-01T10:00:00Z")), "Ship it"
                }),
                "export:audit-log", List.<Object[]>of(new Object[]{
                        "Ada Lovelace", "ADMIN", "COMPANY_DATA_EXPORTED", "{\"fileCount\":8}",
                        Timestamp.from(Instant.parse("2026-05-01T10:00:00Z"))
                })
        ), payrollFixture);
        JpaCompanyDataExportDataProvider provider = new JpaCompanyDataExportDataProvider(
                entityManager,
                new ExportLocalizationService()
        );

        List<ExportWorkbookData> workbooks = provider.loadCompanyData(companyId, "en");

        assertThat(workbooks).extracting(ExportWorkbookData::path).containsExactlyElementsOf(EXPECTED_PATHS);
        for (String path : EXPECTED_PATHS) {
            assertThat(workbook(workbooks, path).rows()).hasSize(1);
        }
        assertThat(workbook(workbooks, "payroll/payroll.xlsx").rows().get(0))
                .containsExactly(
                        "Ada Lovelace",
                        "Employee / Full Time",
                        "Fixed Monthly",
                        new BigDecimal("1000.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("50.00"),
                        new BigDecimal("1100.00"),
                        "Calculated"
                );
    }

    private EntityManager entityManagerProxy(
            List<QueryCall> issuedQueries,
            Map<String, List<Object[]>> nativeRowsBySqlToken
    ) {
        return entityManagerProxy(issuedQueries, nativeRowsBySqlToken, new PayrollFixture(List.of(), List.of()));
    }

    private EntityManager entityManagerProxy(
            List<QueryCall> issuedQueries,
            Map<String, List<Object[]>> nativeRowsBySqlToken,
            PayrollFixture payrollFixture
    ) {
        return (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    if (isNativeCreateQuery(method, args)) {
                        QueryCall call = new QueryCall((String) args[0], new LinkedHashMap<>());
                        issuedQueries.add(call);
                        return queryProxy(call, nativeRowsFor(call.sql(), nativeRowsBySqlToken));
                    }
                    if (isTypedCreateQuery(method, args)) {
                        QueryCall call = new QueryCall((String) args[0], new LinkedHashMap<>());
                        issuedQueries.add(call);
                        return typedQueryProxy(call, typedRowsFor(call.sql(), payrollFixture));
                    }
                    if ("isOpen".equals(method.getName())) {
                        return true;
                    }
                    if ("toString".equals(method.getName())) {
                        return "EntityManager export-test proxy";
                    }
                    throw new UnsupportedOperationException(method.toString());
                }
        );
    }

    private List<Object[]> nativeRowsFor(String sql, Map<String, List<Object[]>> nativeRowsBySqlToken) {
        String normalizedSql = sql.toLowerCase();
        return nativeRowsBySqlToken.entrySet().stream()
                .filter(entry -> normalizedSql.contains(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(List.of());
    }

    private List<?> typedRowsFor(String jpql, PayrollFixture payrollFixture) {
        if (jpql.contains("from PayrollResult")) {
            return payrollFixture.results();
        }
        if (jpql.contains("from PayrollAdjustment")) {
            return payrollFixture.adjustments();
        }
        return List.of();
    }

    private boolean isNativeCreateQuery(Method method, Object[] args) {
        return "createNativeQuery".equals(method.getName())
                && args != null
                && args.length == 1
                && args[0] instanceof String;
    }

    private boolean isTypedCreateQuery(Method method, Object[] args) {
        return "createQuery".equals(method.getName())
                && args != null
                && args.length == 2
                && args[0] instanceof String
                && args[1] instanceof Class<?>;
    }

    private Query queryProxy(QueryCall call, List<Object[]> resultRows) {
        return (Query) queryProxyFor(call, resultRows, Query.class);
    }

    private TypedQuery<?> typedQueryProxy(QueryCall call, List<?> resultRows) {
        return (TypedQuery<?>) queryProxyFor(call, resultRows, TypedQuery.class);
    }

    private Object queryProxyFor(QueryCall call, List<?> resultRows, Class<?> queryType) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("setParameter".equals(method.getName())
                    && args != null
                    && args.length >= 2
                    && args[0] instanceof String parameterName) {
                call.parameters().put(parameterName, args[1]);
                return proxy;
            }
            if ("getResultList".equals(method.getName())) {
                return filteredResultRows(call, resultRows);
            }
            if ("toString".equals(method.getName())) {
                return "Query export-test proxy";
            }
            throw new UnsupportedOperationException(method.toString());
        };
        return Proxy.newProxyInstance(
                Query.class.getClassLoader(),
                new Class<?>[]{queryType},
                handler
        );
    }

    private List<?> filteredResultRows(QueryCall call, List<?> resultRows) {
        Object type = call.parameters().get("type");
        if (type instanceof PayrollAdjustmentType adjustmentType
                && resultRows.stream().allMatch(PayrollAdjustment.class::isInstance)) {
            return resultRows.stream()
                    .map(PayrollAdjustment.class::cast)
                    .filter(adjustment -> adjustment.getType() == adjustmentType)
                    .toList();
        }
        return resultRows;
    }

    private ExportWorkbookData workbook(List<ExportWorkbookData> workbooks, String path) {
        return workbooks.stream()
                .filter(workbook -> workbook.path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private PayrollFixture payrollFixture(UUID companyId) {
        Company company = new Company();
        company.setId(companyId);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setDisplayName("Ada Lovelace");
        user.setEmail("ada@example.test");

        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setCompany(company);
        employee.setUser(user);
        employee.setEmploymentTypeRole(PlatformRole.EMPLOYEE);
        employee.setEmploymentType(EmploymentType.FULL_TIME);
        employee.setPaymentMethod(PaymentMethod.FIXED_MONTHLY);

        PayrollResult result = new PayrollResult();
        result.setId(UUID.randomUUID());
        result.setCompany(company);
        result.setEmployee(employee);
        result.setYear(2026);
        result.setMonth(5);
        result.setStatus(PayrollStatus.CALCULATED);
        result.setBasePay(new BigDecimal("1000.00"));
        result.setTotalDeductions(new BigDecimal("250.00"));
        result.setGrossEarnings(new BigDecimal("1100.00"));

        PayrollAdjustment bonus = adjustment(company, employee, PayrollAdjustmentType.BONUS, "100.00");
        PayrollAdjustment deduction = adjustment(company, employee, PayrollAdjustmentType.DEDUCTION, "50.00");
        return new PayrollFixture(List.of(result), List.of(bonus, deduction));
    }

    private PayrollAdjustment adjustment(
            Company company,
            Employee employee,
            PayrollAdjustmentType type,
            String amount
    ) {
        PayrollAdjustment adjustment = new PayrollAdjustment();
        adjustment.setId(UUID.randomUUID());
        adjustment.setCompany(company);
        adjustment.setEmployee(employee);
        adjustment.setYear(2026);
        adjustment.setMonth(5);
        adjustment.setType(type);
        adjustment.setAmount(new BigDecimal(amount));
        return adjustment;
    }

    private record QueryCall(
            String sql,
            Map<String, Object> parameters
    ) {
    }

    private record PayrollFixture(
            List<PayrollResult> results,
            List<PayrollAdjustment> adjustments
    ) {
    }
}
