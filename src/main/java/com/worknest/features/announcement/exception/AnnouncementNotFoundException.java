package com.worknest.features.announcement.exception;

import com.worknest.common.exception.ResourceNotFoundException;

public class AnnouncementNotFoundException extends ResourceNotFoundException {
    public AnnouncementNotFoundException() {
        super("ANNOUNCEMENT_NOT_FOUND", "Announcement not found or access denied.");
    }
}