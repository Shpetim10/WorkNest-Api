package com.worknest.features.company.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
        assertThat(issuedQueries).hasSize(EXPECTED_PATHS.size());
        assertThat(issuedQueries).allSatisfy(query ->
                assertThat(query.parameters()).containsEntry("companyId", companyId)
        );
    }

    @Test
    void loadCompanyDataAddsRowsForEveryDatabaseBackedModuleWorkbook() {
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
                "export:payroll", List.<Object[]>of(new Object[]{
                        "Ada Lovelace", "EMPLOYEE", "FULL_TIME", "FIXED_MONTHLY",
                        BigDecimal.valueOf(1000), BigDecimal.valueOf(100),
                        BigDecimal.valueOf(50), BigDecimal.valueOf(1100), "CALCULATED"
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
        ));
        JpaCompanyDataExportDataProvider provider = new JpaCompanyDataExportDataProvider(
                entityManager,
                new ExportLocalizationService()
        );

        List<ExportWorkbookData> workbooks = provider.loadCompanyData(UUID.randomUUID(), "en");

        assertThat(workbooks).extracting(ExportWorkbookData::path).containsExactlyElementsOf(EXPECTED_PATHS);
        for (String path : EXPECTED_PATHS) {
            assertThat(workbook(workbooks, path).rows()).hasSize(1);
        }
    }

    private EntityManager entityManagerProxy(
            List<QueryCall> issuedQueries,
            Map<String, List<Object[]>> nativeRowsBySqlToken
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

    private boolean isNativeCreateQuery(Method method, Object[] args) {
        return "createNativeQuery".equals(method.getName())
                && args != null
                && args.length == 1
                && args[0] instanceof String;
    }

    private Query queryProxy(QueryCall call, List<Object[]> resultRows) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("setParameter".equals(method.getName())
                    && args != null
                    && args.length >= 2
                    && args[0] instanceof String parameterName) {
                call.parameters().put(parameterName, args[1]);
                return proxy;
            }
            if ("getResultList".equals(method.getName())) {
                return resultRows;
            }
            if ("toString".equals(method.getName())) {
                return "Query export-test proxy";
            }
            throw new UnsupportedOperationException(method.toString());
        };
        return (Query) Proxy.newProxyInstance(
                Query.class.getClassLoader(),
                new Class<?>[]{Query.class},
                handler
        );
    }

    private ExportWorkbookData workbook(List<ExportWorkbookData> workbooks, String path) {
        return workbooks.stream()
                .filter(workbook -> workbook.path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private record QueryCall(
            String sql,
            Map<String, Object> parameters
    ) {
    }
}
