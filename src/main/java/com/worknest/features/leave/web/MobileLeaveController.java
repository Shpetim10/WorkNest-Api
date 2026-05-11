package com.worknest.features.leave.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.features.leave.application.LeaveService;
import com.worknest.features.leave.dto.CreateLeaveRequestDto;
import com.worknest.features.leave.dto.LeaveBalanceDto;
import com.worknest.features.leave.dto.LeaveRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mobile/leave")
@Tag(name = "Mobile Leave", description = "Self-service leave balance and request APIs for the employee mobile app.")
public class MobileLeaveController {

    private final LeaveService leaveService;

    @GetMapping("/balance")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get current employee's leave balance for this year")
    public ApiResponse<List<LeaveBalanceDto>> balance() {
        return ApiResponse.success("Leave balance loaded", leaveService.getMyBalance());
    }

    @GetMapping("/requests")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get current employee's leave request history")
    public ApiResponse<PaginatedResponse<LeaveRequestDto>> myRequests(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Leave requests loaded",
                PaginatedResponse.from(leaveService.getMyRequests(PaginationSupport.pageable(page, size)))
        );
    }

    @PostMapping("/requests")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Submit a new leave request")
    public ApiResponse<Void> submit(@Valid @RequestBody CreateLeaveRequestDto request) {
        leaveService.submitRequest(request);
        return ApiResponse.success("Leave request submitted successfully", null);
    }
}
