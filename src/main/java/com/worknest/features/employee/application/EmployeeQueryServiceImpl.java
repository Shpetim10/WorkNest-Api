package com.worknest.features.employee.application;

import com.worknest.common.exception.ResourceNotFoundException;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.RoleAssignmentPermission;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.RoleAssignmentPermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.employee.dto.*;
import com.worknest.features.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeQueryServiceImpl implements EmployeeQueryService {

    private final EmployeeRepository employeeRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StaffListResponse> listStaff(UUID companyId) {
        List<PlatformRole> roles = List.of(PlatformRole.STAFF, PlatformRole.ADMIN, PlatformRole.SUPERADMIN);
        return employeeRepository.findAllByCompanyIdAndEmploymentTypeRoleIn(companyId, roles)
                .stream()
                .map(this::mapToStaffResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeListResponse> listEmployees(UUID companyId) {
        List<PlatformRole> roles = List.of(PlatformRole.EMPLOYEE);
        return employeeRepository.findAllByCompanyIdAndEmploymentTypeRoleIn(companyId, roles)
                .stream()
                .map(this::mapToEmployeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeListResponse> listUnassignedEmployees(UUID companyId, UUID departmentId) {
        return employeeRepository.findUnassignedEmployeesByDepartment(companyId, PlatformRole.EMPLOYEE, departmentId)
                .stream()
                .map(this::mapToEmployeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeListResponse> listAssignedEmployees(UUID companyId, UUID departmentId, UUID supervisorRoleAssignmentId) {
        return employeeRepository.findAssignedEmployeesByDepartmentAndSupervisor(
                        companyId, PlatformRole.EMPLOYEE, departmentId, supervisorRoleAssignmentId)
                .stream()
                .map(this::mapToEmployeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffLookup> lookupStaff(UUID companyId, UUID departmentId) {
        List<PlatformRole> roles = List.of(PlatformRole.STAFF, PlatformRole.ADMIN, PlatformRole.SUPERADMIN);
        return employeeRepository.findByCompanyAndRolesAndDepartment(companyId, roles, departmentId)
                .stream()
                .map(e -> {
                    String fullName = (e.getUser() != null)
                            ? (e.getUser().getFirstName() + " " + e.getUser().getLastName()).trim()
                            : "Unknown";
                    UUID raId = (e.getUser() != null)
                            ? roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(e.getUser().getId(), companyId)
                                .map(com.worknest.domain.entities.RoleAssignment::getId)
                                .orElse(null)
                            : null;
                    return new StaffLookup(raId, fullName);
                })
                .filter(sl -> sl.id() != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailsResponse getEmployee(UUID companyId, UUID employeeId) {
        Employee e = employeeRepository.findById(employeeId)
                .filter(emp -> emp.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (e.getEmploymentTypeRole() != PlatformRole.EMPLOYEE) {
            throw new ResourceNotFoundException("Target record is not an EMPLOYEE");
        }

        String jobTitle = (e.getUser() != null)
                ? roleAssignmentRepository.findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(e.getUser().getId(), e.getCompany().getId())
                    .map(com.worknest.domain.entities.RoleAssignment::getJobTitle)
                    .orElse(null)
                : null;

        var supervisorRa = e.getSupervisorRoleAssignment();
        String supervisorName = (supervisorRa != null && supervisorRa.getUser() != null)
                ? (supervisorRa.getUser().getFirstName() + " " + supervisorRa.getUser().getLastName()).trim()
                : null;
        String supervisorJobTitle = (supervisorRa != null) ? supervisorRa.getJobTitle() : null;

        return new EmployeeDetailsResponse(
                e.getId(),
                e.getUser() != null ? e.getUser().getId() : null,
                e.getUser() != null ? e.getUser().getFirstName() : null,
                e.getUser() != null ? e.getUser().getLastName() : null,
                e.getUser() != null ? e.getUser().getEmail() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                jobTitle,
                e.getCompanySite() != null ? e.getCompanySite().getName() : null,
                e.getCompanySite() != null ? e.getCompanySite().getId() : null,
                e.getStartDate(),
                e.getEmploymentStatus(),
                supervisorRa != null ? supervisorRa.getId() : null,
                supervisorName,
                supervisorJobTitle,
                e.getEmploymentType(),
                e.getContractDocumentKey(),
                e.getContractDocumentPath(),
                e.getContractExpiryDate(),
                e.getLeaveDaysPerYear(),
                e.getPaymentMethod(),
                e.getMonthlySalary(),
                e.getHourlyRate()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDetailsResponse getStaff(UUID companyId, UUID staffId) {
        Employee e = employeeRepository.findById(staffId)
                .filter(emp -> emp.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (e.getEmploymentTypeRole() == PlatformRole.EMPLOYEE) {
            throw new ResourceNotFoundException("Target record is a basic EMPLOYEE, not Staff");
        }

        com.worknest.domain.entities.RoleAssignment ra = roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(e.getUser().getId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Role assignment not found for staff member"));

        List<String> permissionCodes = roleAssignmentPermissionRepository
                .findAllByRoleAssignmentIdAndIsGranted(ra.getId(), true)
                .stream()
                .map(rap -> rap.getPermission().getCode())
                .collect(Collectors.toList());

        List<EmployeeSummaryDto> assignedEmployees = employeeRepository.findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE, ra.getId())
                .stream()
                .map(emp -> {
                    String empJobTitle = roleAssignmentRepository.findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(emp.getUser().getId(), companyId)
                            .map(com.worknest.domain.entities.RoleAssignment::getJobTitle)
                            .orElse(null);
                    return new EmployeeSummaryDto(
                            emp.getId(), emp.getUser().getId(),
                            emp.getUser().getFirstName(), emp.getUser().getLastName(),
                            emp.getUser().getEmail(),
                            emp.getDepartment() != null ? emp.getDepartment().getName() : null,
                            empJobTitle
                    );
                })
                .collect(Collectors.toList());

        long assignedEmployeesCount = employeeRepository.countBySupervisorRoleAssignmentId(ra.getId());

        return new StaffDetailsResponse(
                e.getId(),
                e.getUser() != null ? e.getUser().getId() : null,
                e.getUser() != null ? e.getUser().getFirstName() : null,
                e.getUser() != null ? e.getUser().getLastName() : null,
                e.getUser() != null ? e.getUser().getEmail() : null,
                ra.getJobTitle(),
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getCompanySite() != null ? e.getCompanySite().getName() : null,
                e.getCompanySite() != null ? e.getCompanySite().getId() : null,
                e.getEmploymentTypeRole(),
                e.getStartDate(),
                e.getEmploymentStatus(),
                permissionCodes,
                assignedEmployeesCount,
                assignedEmployees,
                e.getEmploymentType(),
                e.getContractDocumentKey(),
                e.getContractExpiryDate(),
                e.getLeaveDaysPerYear(),
                e.getPaymentMethod(),
                e.getMonthlySalary(),
                e.getHourlyRate()
        );
    }

    private StaffListResponse mapToStaffResponse(Employee e) {
        var ra = roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(e.getUser().getId(), e.getCompany().getId())
                .orElse(null);

        String jobTitle = (ra != null) ? ra.getJobTitle() : null;
        long assignedEmployeesCount = (ra != null) ? employeeRepository.countBySupervisorRoleAssignmentId(ra.getId()) : 0;

        List<String> permissionCodes = (ra != null)
                ? roleAssignmentPermissionRepository.findAllByRoleAssignmentIdAndIsGranted(ra.getId(), true)
                .stream()
                .map(rap -> rap.getPermission().getCode())
                .collect(Collectors.toList())
                : List.of();

        return new StaffListResponse(
                e.getId(),
                (ra != null) ? ra.getId() : null,
                e.getUser() != null ? e.getUser().getId() : null,
                e.getUser() != null ? e.getUser().getFirstName() : null,
                e.getUser() != null ? e.getUser().getLastName() : null,
                e.getUser() != null ? e.getUser().getEmail() : null,
                jobTitle,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getCompanySite() != null ? e.getCompanySite().getName() : null,
                e.getCompanySite() != null ? e.getCompanySite().getId() : null,
                e.getEmploymentTypeRole(),
                e.getStartDate(),
                e.getEmploymentStatus(),
                assignedEmployeesCount,
                permissionCodes
        );
    }

    private EmployeeListResponse mapToEmployeeResponse(Employee e) {
        String name = (e.getUser() != null) ? (e.getUser().getFirstName() + " " + e.getUser().getLastName()).trim() : null;
        String email = (e.getUser() != null) ? e.getUser().getEmail() : null;

        String jobTitle = (e.getUser() != null)
                ? roleAssignmentRepository.findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(e.getUser().getId(), e.getCompany().getId())
                    .map(com.worknest.domain.entities.RoleAssignment::getJobTitle)
                    .orElse(null)
                : null;

        return new EmployeeListResponse(
                e.getId(),
                e.getUser() != null ? e.getUser().getId() : null,
                e.getUser() != null ? e.getUser().getFirstName() : null,
                e.getUser() != null ? e.getUser().getLastName() : null,
                name,
                email,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                jobTitle,
                e.getCompanySite() != null ? e.getCompanySite().getName() : null,
                e.getCompanySite() != null ? e.getCompanySite().getId() : null,
                e.getStartDate(),
                e.getEmploymentStatus()
        );
    }
}
