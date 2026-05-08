package com.worknest.realtime.publisher;

import com.worknest.realtime.event.DepartmentCreatedDomainEvent;
import com.worknest.realtime.event.DepartmentDeletedDomainEvent;
import com.worknest.realtime.event.DepartmentEventType;
import com.worknest.realtime.event.DepartmentUpdatedDomainEvent;
import com.worknest.realtime.event.RealtimeEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DepartmentRealtimeEventListener {

    private final RealtimeEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepartmentCreated(DepartmentCreatedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(DepartmentEventType.DEPARTMENT_CREATED)
                .entity("department")
                .entityId(event.departmentId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(event.snapshot())
                .build();
        publisher.publishToCompanyDepartments(event.companyId(), envelope);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepartmentUpdated(DepartmentUpdatedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(DepartmentEventType.DEPARTMENT_UPDATED)
                .entity("department")
                .entityId(event.departmentId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(event.snapshot())
                .build();
        publisher.publishToCompanyDepartments(event.companyId(), envelope);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepartmentDeleted(DepartmentDeletedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(DepartmentEventType.DEPARTMENT_DELETED)
                .entity("department")
                .entityId(event.departmentId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(Map.of("name", event.name()))
                .build();
        publisher.publishToCompanyDepartments(event.companyId(), envelope);
    }
}
