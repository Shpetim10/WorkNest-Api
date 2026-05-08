package com.worknest.realtime.publisher;

import com.worknest.realtime.event.CompanySettingsEventType;
import com.worknest.realtime.event.CompanySettingsUpdatedDomainEvent;
import com.worknest.realtime.event.RealtimeEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CompanySettingsRealtimeEventListener {

    private final RealtimeEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompanySettingsUpdated(CompanySettingsUpdatedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(CompanySettingsEventType.COMPANY_SETTINGS_UPDATED)
                .entity("companySettings")
                .entityId(event.companyId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(event.snapshot())
                .build();
        publisher.publishToCompanySettings(event.companyId(), envelope);
    }
}
