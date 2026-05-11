package com.worknest.features.leave.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.domain.enums.LeaveStatus;
import com.worknest.features.leave.application.LeaveService;
import com.worknest.features.leave.dto.LeaveRequestDto;
import com.worknest.features.leave.dto.RejectLeaveRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/leave")
@Tag(name = "Admin Leave", description = "Leave request management for admins and managers.")
public class AdminLeaveController {

    private final LeaveService leaveService;

    @GetMapping("/requests")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "List all company leave requests — filterable by status and employee name")
    public ApiResponse<PaginatedResponse<LeaveRequestDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Leave requests loaded",
                PaginatedResponse.from(leaveService.adminListRequests(search, status, PaginationSupport.pageable(page, size)))
        );
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "View a single leave request in detail")
    public ApiResponse<LeaveRequestDto> get(@PathVariable UUID requestId) {
        return ApiResponse.success("Leave request loaded", leaveService.adminGetRequest(requestId));
    }

    @PostMapping("/requests/{requestId}/approve")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Approve a pending leave request")
    public ApiResponse<Void> approve(@PathVariable UUID requestId) {
        leaveService.approveRequest(requestId);
        return ApiResponse.success("Leave request approved", null);
    }

    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Reject a pending leave request with a reason")
    public ApiResponse<Void> reject(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectLeaveRequestDto dto
    ) {
        leaveService.rejectRequest(requestId, dto);
        return ApiResponse.success("Leave request rejected", null);
    }
}
