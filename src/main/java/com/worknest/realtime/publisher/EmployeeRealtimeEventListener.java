package com.worknest.realtime.publisher;

import com.worknest.realtime.event.EmployeeEventType;
import com.worknest.realtime.event.EmployeeProvisionedDomainEvent;
import com.worknest.realtime.event.RealtimeEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmployeeRealtimeEventListener {

    private final RealtimeEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmployeeProvisioned(EmployeeProvisionedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(EmployeeEventType.EMPLOYEE_PROVISIONED)
                .entity("employee")
                .entityId(event.employeeId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(Map.of(
                        "employeeId", event.employeeId(),
                        "role", event.role(),
                        "email", event.email()
                ))
                .build();
        publisher.publishToCompanyEmployees(event.companyId(), envelope);
    }
}
