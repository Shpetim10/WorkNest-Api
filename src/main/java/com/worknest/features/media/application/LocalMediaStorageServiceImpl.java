package com.worknest.features.media.application;

import com.worknest.domain.enums.MediaCategory;
import com.worknest.features.media.dto.MediaUploadResponse;
import com.worknest.features.media.exception.InvalidConfigurationRequestException;
import com.worknest.features.media.application.MediaStorageService;
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

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> DOCUMENT_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Path storageRoot;

    public LocalMediaStorageServiceImpl(@Value("${app.media.local-root:storage/media}") String storageRoot) {
        this.storageRoot = Paths.get(storageRoot).normalize();
    }

    @Override
    public MediaUploadResponse upload(UUID companyId, UUID userId, MediaCategory category, MultipartFile file) {
        validateFile(file, category);

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
            case USER_PROFILE -> (companyId == null || userId == null)
                ? String.format(
                    Locale.ROOT,
                    "public/users/profile/%04d/%02d/%s.%s",
                    today.getYear(),
                    today.getMonthValue(),
                    UUID.randomUUID(),
                    extension
                )
                : String.format(
                    Locale.ROOT,
                    "companies/%s/users/%s/profile/%04d/%02d/%s.%s",
                    companyId,
                    userId,
                    today.getYear(),
                    today.getMonthValue(),
                    UUID.randomUUID(),
                    extension
                );
            case REGISTRATION_LOGO -> String.format(
                    Locale.ROOT,
                    "public/registrations/logos/%04d/%02d/%s.%s",
                    today.getYear(),
                    today.getMonthValue(),
                    UUID.randomUUID(),
                    extension
            );
            case EMPLOYEE_CONTRACT -> String.format(
                    Locale.ROOT,
                    "companies/%s/employees/contracts/%04d/%02d/%s.%s",
                    companyId,
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

    @Override
    public MediaUploadResponse uploadPublic(MediaCategory category, MultipartFile file) {
        if (category != MediaCategory.REGISTRATION_LOGO && category != MediaCategory.USER_PROFILE) {
            throw new InvalidConfigurationRequestException("Only REGISTRATION_LOGO and USER_PROFILE are allowed for public uploads");
        }
        return upload(null, null, category, file);
    }

    @Override
    public MediaUploadResponse promoteLogo(String logoKey, UUID companyId) {
        if (!StringUtils.hasText(logoKey) || !logoKey.startsWith("public/registrations/logos/")) {
            throw new InvalidConfigurationRequestException("Invalid registration logo key");
        }

        Path source = storageRoot.resolve(logoKey).normalize();
        if (!Files.exists(source)) {
            throw new InvalidConfigurationRequestException("Registration logo file not found");
        }

        LocalDate today = LocalDate.now();
        String extension = logoKey.substring(logoKey.lastIndexOf('.') + 1);
        String newKey = String.format(
                Locale.ROOT,
                "companies/%s/branding/logo/%04d/%02d/%s.%s",
                companyId,
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                extension
        );

        Path target = storageRoot.resolve(newKey).normalize();

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new InvalidConfigurationRequestException("Failed to promote registration logo to company directory");
        }

        return new MediaUploadResponse(
                newKey,
                target.toString().replace('\\', '/'),
                source.getFileName().toString(),
                "image/" + extension,
                target.toFile().length(),
                "Logo promoted successfully"
        );
    }

    @Override
    public MediaUploadResponse promoteProfileImage(String key, UUID companyId, UUID userId) {
        if (!StringUtils.hasText(key) || !key.contains("public/")) {
            throw new InvalidConfigurationRequestException("Invalid temporary profile image key");
        }

        Path source = storageRoot.resolve(key).normalize();
        if (!Files.exists(source)) {
            throw new InvalidConfigurationRequestException("Profile image file not found");
        }

        LocalDate today = LocalDate.now();
        String extension = key.substring(key.lastIndexOf('.') + 1);
        String newKey = String.format(
                Locale.ROOT,
                "companies/%s/users/%s/profile/%04d/%02d/%s.%s",
                companyId,
                userId,
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                extension
        );

        Path target = storageRoot.resolve(newKey).normalize();

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new InvalidConfigurationRequestException("Failed to promote profile image to user directory");
        }

        return new MediaUploadResponse(
                newKey,
                target.toString().replace('\\', '/'),
                source.getFileName().toString(),
                "image/" + extension,
                target.toFile().length(),
                "Profile image promoted successfully"
        );
    }

    private void validateFile(MultipartFile file, MediaCategory category) {
        if (file == null || file.isEmpty()) {
            throw new InvalidConfigurationRequestException("file is required");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new InvalidConfigurationRequestException("file name is required");
        }
        if (category == MediaCategory.EMPLOYEE_CONTRACT) {
            if (!DOCUMENT_CONTENT_TYPES.contains(file.getContentType())) {
                throw new InvalidConfigurationRequestException("Only PDF and DOCX documents are supported for employee contracts");
            }
        } else {
            if (!IMAGE_CONTENT_TYPES.contains(file.getContentType())) {
                throw new InvalidConfigurationRequestException("Only JPEG, PNG, and WEBP images are supported");
            }
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
            case "application/pdf" -> "pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            default -> throw new InvalidConfigurationRequestException("Unsupported media type");
        };
    }
}
