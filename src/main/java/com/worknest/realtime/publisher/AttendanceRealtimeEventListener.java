package com.worknest.realtime.publisher;

import com.worknest.realtime.event.AttendanceDayAdjustedDomainEvent;
import com.worknest.realtime.event.AttendanceEventReviewedDomainEvent;
import com.worknest.realtime.event.AttendanceManualEventDomainEvent;
import com.worknest.realtime.event.RealtimeEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AttendanceRealtimeEventListener {

    private final RealtimeEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onManualEvent(AttendanceManualEventDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(event.realtimeEventType())
                .entity("attendanceEvent")
                .entityId(event.employeeId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(Map.of(
                        "employeeId", event.employeeId(),
                        "occurredAt", event.occurredAt()
                ))
                .build();
        publisher.publishToCompanyAttendance(event.companyId(), envelope);
        // Also notify the affected employee directly: they are not authorized to
        // subscribe to the company-wide attendance topic (STAFF/ADMIN only).
        if (event.employeeUserId() != null) {
            publisher.publishToUser(event.employeeUserId(), envelope);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventReviewed(AttendanceEventReviewedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type("ATTENDANCE_EVENT_REVIEWED")
                .entity("attendanceEvent")
                .entityId(event.eventId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(Map.of(
                        "eventId", event.eventId(),
                        "employeeId", event.employeeId(),
                        "reviewStatus", event.reviewStatus()
                ))
                .build();
        publisher.publishToCompanyAttendance(event.companyId(), envelope);
        if (event.employeeUserId() != null) {
            publisher.publishToUser(event.employeeUserId(), envelope);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDayAdjusted(AttendanceDayAdjustedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type("ATTENDANCE_DAY_ADJUSTED")
                .entity("attendanceDayRecord")
                .entityId(event.recordId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(Map.of(
                        "recordId", event.recordId(),
                        "employeeId", event.employeeId(),
                        "workDate", event.workDate()
                ))
                .build();
        publisher.publishToCompanyAttendance(event.companyId(), envelope);
        if (event.employeeUserId() != null) {
            publisher.publishToUser(event.employeeUserId(), envelope);
        }
    }
}
