package com.worknest.audit.service.impl;

import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.repository.PlatformEventRepository;
import com.worknest.audit.service.PlatformEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformEventServiceImpl implements PlatformEventService {

    private final PlatformEventRepository platformEventRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEvent(String eventType, UUID companyId, String description) {
        log.debug("Publishing platform event: {} for company: {}", eventType, companyId);
        
        // TODO: Resolve current user and company details
        // TODO: Build PlatformEvent entity
        // TODO: Save to repository
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEvent(PlatformEvent event) {
        log.debug("Persisting pre-constructed platform event: {}", event.getEventType());
        platformEventRepository.save(event);
    }
}
