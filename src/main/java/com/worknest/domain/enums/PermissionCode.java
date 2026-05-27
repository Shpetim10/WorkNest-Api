package com.worknest.domain.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum PermissionCode {
    EMPLOYEE_VIEW("EMPLOYEE", "VIEW", "View employee records"),
    EMPLOYEE_CREATE("EMPLOYEE", "CREATE", "Create employee records"),
    EMPLOYEE_UPDATE("EMPLOYEE", "UPDATE", "Update employee records"),
    EMPLOYEE_DELETE("EMPLOYEE", "DELETE", "Delete employee records"),
    EMPLOYEE_ASSIGNMENT_VIEW("EMPLOYEE_ASSIGNMENT", "VIEW", "View employee manager assignments"),
    EMPLOYEE_ASSIGNMENT_UPDATE("EMPLOYEE_ASSIGNMENT", "UPDATE", "Update employee manager assignments"),

    STAFF_VIEW("STAFF", "VIEW", "View staff records"),
    STAFF_CREATE("STAFF", "CREATE", "Create staff records"),
    STAFF_UPDATE("STAFF", "UPDATE", "Update staff records"),
    STAFF_DELETE("STAFF", "DELETE", "Delete staff records"),
    STAFF_PERMISSION_MANAGE("STAFF_PERMISSION", "MANAGE", "Manage staff permissions"),

    DEPARTMENT_VIEW("DEPARTMENT", "VIEW", "View departments"),
    DEPARTMENT_CREATE("DEPARTMENT", "CREATE", "Create departments"),
    DEPARTMENT_UPDATE("DEPARTMENT", "UPDATE", "Update departments"),
    DEPARTMENT_DELETE("DEPARTMENT", "DELETE", "Delete departments"),

    COMPANY_SITE_VIEW("COMPANY_SITE", "VIEW", "View company sites"),
    COMPANY_SITE_CREATE("COMPANY_SITE", "CREATE", "Create company sites"),
    COMPANY_SITE_UPDATE("COMPANY_SITE", "UPDATE", "Update company sites"),
    COMPANY_SITE_DELETE("COMPANY_SITE", "DELETE", "Delete company sites"),

    COMPANY_SETTINGS_VIEW("COMPANY_SETTINGS", "VIEW", "View company settings"),
    COMPANY_SETTINGS_UPDATE("COMPANY_SETTINGS", "UPDATE", "Update company settings"),
    COMPANY_DATA_EXPORT("COMPANY_DATA", "EXPORT", "Export company data"),

    ATTENDANCE_VIEW("ATTENDANCE", "VIEW", "View attendance records"),
    ATTENDANCE_CREATE("ATTENDANCE", "CREATE", "Create attendance records"),
    ATTENDANCE_UPDATE("ATTENDANCE", "UPDATE", "Update attendance records"),
    ATTENDANCE_REVIEW("ATTENDANCE", "REVIEW", "Review attendance events"),
    ATTENDANCE_POLICY_VIEW("ATTENDANCE_POLICY", "VIEW", "View attendance policies"),
    ATTENDANCE_POLICY_UPDATE("ATTENDANCE_POLICY", "UPDATE", "Update attendance policies"),
    QR_TERMINAL_VIEW("QR_TERMINAL", "VIEW", "View QR terminals"),
    QR_TERMINAL_CREATE("QR_TERMINAL", "CREATE", "Create QR terminals"),
    QR_TERMINAL_UPDATE("QR_TERMINAL", "UPDATE", "Update QR terminals"),

    LEAVE_VIEW("LEAVE", "VIEW", "View leave requests"),
    LEAVE_CREATE("LEAVE", "CREATE", "Create leave requests"),
    LEAVE_APPROVE("LEAVE", "APPROVE", "Approve or reject leave requests"),

    PAYROLL_VIEW("PAYROLL", "VIEW", "View payroll data"),
    PAYROLL_CALCULATE("PAYROLL", "CALCULATE", "Calculate payroll"),
    PAYROLL_UPDATE("PAYROLL", "UPDATE", "Update payroll adjustments and settings"),
    PAYROLL_APPROVE("PAYROLL", "APPROVE", "Approve and finalize payroll"),

    ANNOUNCEMENT_VIEW("ANNOUNCEMENT", "VIEW", "View announcements"),
    ANNOUNCEMENT_CREATE("ANNOUNCEMENT", "CREATE", "Create announcements"),
    ANNOUNCEMENT_DELETE("ANNOUNCEMENT", "DELETE", "Delete announcements"),

    DASHBOARD_VIEW("DASHBOARD", "VIEW", "View admin dashboard"),
    MEDIA_UPLOAD("MEDIA", "UPLOAD", "Upload protected media"),

    AUDIT_LOG_VIEW("AUDIT_LOG", "VIEW", "View company audit logs");

    private static final Set<String> CODES = Arrays.stream(values())
            .map(PermissionCode::name)
            .collect(Collectors.toUnmodifiableSet());

    private final String feature;
    private final String action;
    private final String description;

    PermissionCode(String feature, String action, String description) {
        this.feature = feature;
        this.action = action;
        this.description = description;
    }

    public String code() {
        return name();
    }

    public String feature() {
        return feature;
    }

    public String action() {
        return action;
    }

    public String description() {
        return description;
    }

    public static boolean isKnown(String code) {
        return code != null && CODES.contains(code.trim().toUpperCase());
    }
}
