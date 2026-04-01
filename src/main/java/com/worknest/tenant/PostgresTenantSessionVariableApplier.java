package com.worknest.tenant;

import com.worknest.common.config.TenantSessionProperties;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresTenantSessionVariableApplier {

    private final TenantSessionProperties tenantSessionProperties;

    public void apply(Connection connection, TenantSessionContext tenantSessionContext) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(buildSetSql(tenantSessionContext));
        }
    }

    public void clear(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(buildResetSql());
        }
    }

    private String buildSetSql(TenantSessionContext tenantSessionContext) {
        return "SET " + tenantSessionProperties.getCurrentCompanySetting() + " = '" + tenantSessionContext.companyId() + "'";
    }

    private String buildResetSql() {
        return "RESET " + tenantSessionProperties.getCurrentCompanySetting();
    }
}
