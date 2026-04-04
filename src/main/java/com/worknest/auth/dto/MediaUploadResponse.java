package com.worknest.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing details of the uploaded media file")
public record MediaUploadResponse(
        @Schema(description = "The unique key used to identify the file in storage")
        String storageKey,

        @Schema(description = "The full storage path or access URL for the file")
        String storagePath,

        @Schema(description = "The original name of the uploaded file", example = "logo.png")
        String originalFilename,

        @Schema(description = "The MIME type of the file", example = "image/png")
        String contentType,

        @Schema(description = "The size of the file in bytes")
        long size,

        @Schema(description = "Success message", example = "File uploaded successfully")
        String message
) {
}
