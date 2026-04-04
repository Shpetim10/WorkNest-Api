package com.worknest.auth.service.impl;

import com.worknest.auth.domain.MediaCategory;
import com.worknest.auth.dto.MediaUploadResponse;
import com.worknest.auth.exception.InvalidConfigurationRequestException;
import com.worknest.auth.service.MediaStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalMediaStorageServiceImpl implements MediaStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path storageRoot;

    public LocalMediaStorageServiceImpl(@Value("${app.media.local-root:storage/media}") String storageRoot) {
        this.storageRoot = Paths.get(storageRoot).normalize();
    }

    @Override
    public MediaUploadResponse upload(UUID companyId, UUID userId, MediaCategory category, MultipartFile file) {
        validateFile(file);

        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        LocalDate today = LocalDate.now();
        String key = switch (category) {
            case COMPANY_LOGO -> String.format(
                    Locale.ROOT,
                    "companies/%s/branding/logo/%04d/%02d/%s.%s",
                    companyId,
                    today.getYear(),
                    today.getMonthValue(),
                    UUID.randomUUID(),
                    extension
            );
            case USER_PROFILE -> String.format(
                    Locale.ROOT,
                    "companies/%s/users/%s/profile/%04d/%02d/%s.%s",
                    companyId,
                    userId,
                    today.getYear(),
                    today.getMonthValue(),
                    UUID.randomUUID(),
                    extension
            );
        };

        Path target = storageRoot.resolve(key).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new InvalidConfigurationRequestException("Resolved media path is outside the configured storage root");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new InvalidConfigurationRequestException("Failed to store media file locally");
        }

        return new MediaUploadResponse(
                key,
                target.toString().replace('\\', '/'),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                "Media uploaded successfully"
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidConfigurationRequestException("file is required");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new InvalidConfigurationRequestException("file name is required");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidConfigurationRequestException("Only JPEG, PNG, and WEBP images are supported");
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
            if (StringUtils.hasText(extension)) {
                return extension;
            }
        }

        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new InvalidConfigurationRequestException("Unsupported media type");
        };
    }
}
