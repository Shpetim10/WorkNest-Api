package com.worknest.realtime.publisher;

import com.worknest.realtime.event.CompanySiteActivatedDomainEvent;
import com.worknest.realtime.event.CompanySiteCreatedDomainEvent;
import com.worknest.realtime.event.CompanySiteDisabledDomainEvent;
import com.worknest.realtime.event.CompanySiteEventType;
import com.worknest.realtime.event.CompanySiteUpdatedDomainEvent;
import com.worknest.realtime.event.RealtimeEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanySiteRealtimeEventListener {

    private final RealtimeEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSiteCreated(CompanySiteCreatedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(CompanySiteEventType.COMPANY_SITE_CREATED)
                .entity("companySite")
                .entityId(event.siteId())
                .scopeId(event.companyId())
                .version(event.version())
                .actorUserId(event.actorUserId())
                .payload(event.snapshot())
                .build();
        publisher.publishToCompanySites(event.companyId(), envelope);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSiteUpdated(CompanySiteUpdatedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(CompanySiteEventType.COMPANY_SITE_UPDATED)
                .entity("companySite")
                .entityId(event.siteId())
                .scopeId(event.companyId())
                .version(event.version())
                .actorUserId(event.actorUserId())
                .payload(event.snapshot())
                .build();
        publisher.publishToCompanySites(event.companyId(), envelope);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSiteActivated(CompanySiteActivatedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(CompanySiteEventType.COMPANY_SITE_ACTIVATED)
                .entity("companySite")
                .entityId(event.siteId())
                .scopeId(event.companyId())
                .version(event.version())
                .actorUserId(event.actorUserId())
                .payload(event.snapshot())
                .build();
        publisher.publishToCompanySites(event.companyId(), envelope);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSiteDisabled(CompanySiteDisabledDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(CompanySiteEventType.COMPANY_SITE_DISABLED)
                .entity("companySite")
                .entityId(event.siteId())
                .scopeId(event.companyId())
                .version(event.version())
                .actorUserId(event.actorUserId())
                .payload(event.snapshot())
                .build();
        publisher.publishToCompanySites(event.companyId(), envelope);
    }
}
