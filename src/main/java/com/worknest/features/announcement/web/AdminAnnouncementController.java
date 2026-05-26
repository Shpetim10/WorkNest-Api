package com.worknest.features.announcement.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.features.announcement.application.AnnouncementService;
import com.worknest.features.announcement.dto.AnnouncementListResponse;
import com.worknest.features.announcement.dto.CreateAnnouncementRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Company announcement management API")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'ANNOUNCEMENT_CREATE')")
    @Operation(summary = "Create announcement", description = "Creates a new company announcement")
    public ApiResponse<AnnouncementListResponse> create(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return ApiResponse.success("Announcement created successfully", announcementService.create(companyId, request));
    }

    @GetMapping
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'ANNOUNCEMENT_VIEW')")
    @Operation(summary = "List announcements", description = "Retrieves all announcements for the company")
    public ApiResponse<PaginatedResponse<AnnouncementListResponse>> list(
            @PathVariable UUID companyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Announcements retrieved successfully",
                PaginatedResponse.from(announcementService.listForAdmin(companyId, PaginationSupport.pageable(page, size)))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'ANNOUNCEMENT_DELETE')")
    @Operation(summary = "Delete announcement", description = "Deletes an announcement")
    public ApiResponse<Void> delete(
            @PathVariable UUID companyId,
            @PathVariable UUID id) {
        announcementService.delete(companyId, id);
        return ApiResponse.success("Announcement deleted successfully", null);
    }
}
