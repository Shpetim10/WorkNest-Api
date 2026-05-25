package com.worknest.domain.entities;

import com.worknest.domain.enums.SubscriptionPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "plan_limits")
public class PlanLimit {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 50)
    private SubscriptionPlan plan;

    @Column(name = "max_employees")
    private Integer maxEmployees;

    @Column(name = "max_managers")
    private Integer maxManagers;

    @Column(name = "max_departments")
    private Integer maxDepartments;

    @Column(name = "max_locations")
    private Integer maxLocations;

    @Column(name = "payroll_enabled", nullable = false)
    private Boolean payrollEnabled;

    @Column(name = "audit_logs_enabled", nullable = false)
    private Boolean auditLogsEnabled;

    @Column(name = "price_monthly_cents", nullable = false)
    private Integer priceMonthlyInCents;
}
