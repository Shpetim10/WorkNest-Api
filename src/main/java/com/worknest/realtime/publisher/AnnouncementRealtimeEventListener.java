package com.worknest.realtime.publisher;

import com.worknest.realtime.event.AnnouncementCreatedDomainEvent;
import com.worknest.realtime.event.AnnouncementDeletedDomainEvent;
import com.worknest.realtime.event.AnnouncementEventType;
import com.worknest.realtime.event.RealtimeEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AnnouncementRealtimeEventListener {

    private final RealtimeEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnnouncementCreated(AnnouncementCreatedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(AnnouncementEventType.ANNOUNCEMENT_CREATED)
                .entity("announcement")
                .entityId(event.announcementId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(event.snapshot())
                .build();
        publisher.publishToCompanyAnnouncements(event.companyId(), envelope);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnnouncementDeleted(AnnouncementDeletedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(AnnouncementEventType.ANNOUNCEMENT_DELETED)
                .entity("announcement")
                .entityId(event.announcementId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .build();
        publisher.publishToCompanyAnnouncements(event.companyId(), envelope);
    }
}
