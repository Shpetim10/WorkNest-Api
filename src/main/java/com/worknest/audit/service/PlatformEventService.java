package com.worknest.audit.service;

import com.worknest.audit.domain.PlatformEvent;
import java.util.UUID;

public interface PlatformEventService {

    /**
     * Publishes a platform event for tracking and notification.
     *
     * @param eventType the type of event
     * @param companyId the company context
     * @param description a human-readable description
     */
    void publishEvent(String eventType, UUID companyId, String description);

    /**
     * Persists a pre-constructed PlatformEvent entity.
     *
     * @param event the platform event entity
     */
    void publishEvent(PlatformEvent event);
}
