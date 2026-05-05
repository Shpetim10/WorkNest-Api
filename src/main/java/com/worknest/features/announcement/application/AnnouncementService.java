package com.worknest.features.announcement.application;

import com.worknest.features.announcement.dto.AnnouncementListResponse;
import com.worknest.features.announcement.dto.CreateAnnouncementRequest;
import com.worknest.features.announcement.dto.MobileAnnouncementDetail;
import com.worknest.features.announcement.dto.MobileAnnouncementListItem;
import com.worknest.features.announcement.dto.UnreadCountResponse;
import java.util.List;
import java.util.UUID;

public interface AnnouncementService {

    AnnouncementListResponse create(UUID companyId, CreateAnnouncementRequest request);

    List<AnnouncementListResponse> listForAdmin(UUID companyId);

    void delete(UUID companyId, UUID id);

    List<MobileAnnouncementListItem> listForEmployee();

    MobileAnnouncementDetail getDetail(UUID id);

    void markAsRead(UUID id);

    UnreadCountResponse getUnreadCount();
}