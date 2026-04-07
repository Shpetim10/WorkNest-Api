package com.worknest.features.department.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.department.application.DepartmentService;
import com.worknest.features.department.dto.CreateDepartmentRequest;
import com.worknest.features.department.dto.DepartmentListResponse;
import com.worknest.features.department.dto.DepartmentLookup;
import com.worknest.features.department.dto.DepartmentResponse;
import com.worknest.features.department.dto.UpdateDepartmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Tenant-scoped department management API")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Create department", description = "Creates a new department for the current company")
    public ApiResponse<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.success("Department created successfully", departmentService.createDepartment(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get department", description = "Retrieves a single department by ID for the current company")
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable UUID id) {
        return ApiResponse.success("Department retrieved successfully", departmentService.getDepartment(id));
    }

    @GetMapping
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "List departments", description = "Retrieves all departments for the current company")
    public ApiResponse<List<DepartmentListResponse>> listDepartments() {
        return ApiResponse.success("Departments retrieved successfully", departmentService.listDepartments());
    }

    @GetMapping("/lookup")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Lookup departments", description = "Retrieves department ID and Name pairs for the current company (e.g. for dropdowns)")
    public ApiResponse<List<DepartmentLookup>> lookupDepartments() {
        return ApiResponse.success("Departments lookup successful", departmentService.lookupDepartments());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Update department", description = "Updates an existing department for the current company")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable UUID id, 
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ApiResponse.success("Department updated successfully", departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Delete department", description = "Hard deletes a department for the current company")
    public ApiResponse<Void> deleteDepartment(@PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ApiResponse.success("Department deleted successfully", null);
    }
}
