package com.worknest.features.announcement.application;

import com.worknest.features.announcement.dto.AnnouncementListResponse;
import com.worknest.features.announcement.dto.CreateAnnouncementRequest;
import com.worknest.features.announcement.dto.MobileAnnouncementDetail;
import com.worknest.features.announcement.dto.MobileAnnouncementListItem;
import com.worknest.features.announcement.dto.UnreadCountResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementService {

    AnnouncementListResponse create(UUID companyId, CreateAnnouncementRequest request);

    Page<AnnouncementListResponse> listForAdmin(UUID companyId, Pageable pageable);

    void delete(UUID companyId, UUID id);

    Page<MobileAnnouncementListItem> listForEmployee(Pageable pageable);

    MobileAnnouncementDetail getDetail(UUID id);

    void markAsRead(UUID id);

    UnreadCountResponse getUnreadCount();
}
