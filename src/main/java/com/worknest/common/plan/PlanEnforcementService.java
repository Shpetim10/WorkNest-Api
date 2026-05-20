package com.worknest.common.plan;

import java.util.UUID;

public interface PlanEnforcementService {

    void assertCanAddEmployee(UUID companyId);

    void assertCanAddManager(UUID companyId);

    void assertCanAddDepartment(UUID companyId);

    void assertCanAddLocation(UUID companyId);

    void assertPayrollEnabled(UUID companyId);

    void assertAuditLogsEnabled(UUID companyId);
}
