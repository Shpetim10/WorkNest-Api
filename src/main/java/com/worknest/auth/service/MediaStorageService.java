package com.worknest.auth.service;

import com.worknest.auth.domain.MediaCategory;
import com.worknest.auth.dto.MediaUploadResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    MediaUploadResponse upload(
            UUID companyId,
            UUID userId,
            MediaCategory category,
            MultipartFile file
    );
}
