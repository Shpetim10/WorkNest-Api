package com.worknest.features.media.web;

import com.worknest.domain.enums.MediaCategory;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.media.dto.MediaUploadResponse;
import com.worknest.features.media.application.MediaStorageService;
import com.worknest.common.api.ApiResponse;
import com.worknest.security.AuthSessionPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Local media upload endpoints for branding and profile images")
public class MediaController {

    private final MediaStorageService mediaStorageService;

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@teamSecurity.hasCurrentCompanyRoleOrPermission('MEDIA_UPLOAD', 'EMPLOYEE')")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File uploaded successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation error or invalid file format",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "User is not authenticated",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions (e.g., non-admin uploading logo)",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal error during file processing or storage",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<MediaUploadResponse>> upload(
            @io.swagger.v3.oas.annotations.Parameter(description = "The business category of the media", example = "USER_PROFILE_PICTURE")
            @RequestParam("category") MediaCategory category,

            @io.swagger.v3.oas.annotations.Parameter(description = "The raw file multipart data")
            @RequestParam("file") MultipartFile file,

            @AuthenticationPrincipal AuthSessionPrincipal sessionPrincipal
    ) {
        if (category == MediaCategory.COMPANY_LOGO
                && sessionPrincipal.role() != PlatformRole.ADMIN
                && sessionPrincipal.role() != PlatformRole.SUPERADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Only admins can upload company logos");
        }
        MediaUploadResponse response = mediaStorageService.upload(
                sessionPrincipal.companyId(),
                sessionPrincipal.userId(),
                category,
                file
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.message(), response));
    }

    @PostMapping(path = "/public/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Publicly upload a registration logo",
            description = """
                    **Public endpoint for Step 0 of company registration.**
                    
                    Allows unauthenticated users to upload a company logo file.
                    Returns a `logoKey` and `logoPath` which must be included in the subsequent 
                    `POST /api/v1/companies/register` request.
                    
                    Only `REGISTRATION_LOGO` category is permitted on this endpoint.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Logo uploaded successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid category or file format",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<MediaUploadResponse>> publicUpload(
            @RequestParam("category") MediaCategory category,
            @RequestParam("file") MultipartFile file
    ) {
        if (category != MediaCategory.REGISTRATION_LOGO && category != MediaCategory.USER_PROFILE) {
            throw new org.springframework.security.access.AccessDeniedException("Only REGISTRATION_LOGO and USER_PROFILE are allowed for public uploads");
        }
        MediaUploadResponse response = mediaStorageService.uploadPublic(category, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.message(), response));
    }
}
