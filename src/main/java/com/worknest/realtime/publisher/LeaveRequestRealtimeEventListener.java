package com.worknest.realtime.publisher;

import com.worknest.realtime.event.LeaveRequestApprovedDomainEvent;
import com.worknest.realtime.event.LeaveRequestEventType;
import com.worknest.realtime.event.LeaveRequestRejectedDomainEvent;
import com.worknest.realtime.event.LeaveRequestSubmittedDomainEvent;
import com.worknest.realtime.event.RealtimeEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LeaveRequestRealtimeEventListener {

    private final RealtimeEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaveRequestSubmitted(LeaveRequestSubmittedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(LeaveRequestEventType.LEAVE_REQUEST_SUBMITTED)
                .entity("leaveRequest")
                .entityId(event.leaveRequestId())
                .scopeId(event.companyId())
                .actorUserId(event.employeeUserId())
                .payload(Map.of(
                        "leaveRequestId", event.leaveRequestId(),
                        "employeeId", event.employeeId(),
                        "leaveType", event.leaveType(),
                        "startDate", event.startDate(),
                        "endDate", event.endDate(),
                        "totalDays", event.totalDays()
                ))
                .build();
        publisher.publishToCompanyLeaveRequests(event.companyId(), envelope);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaveRequestApproved(LeaveRequestApprovedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(LeaveRequestEventType.LEAVE_REQUEST_APPROVED)
                .entity("leaveRequest")
                .entityId(event.leaveRequestId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(Map.of(
                        "leaveRequestId", event.leaveRequestId(),
                        "employeeId", event.employeeId(),
                        "leaveType", event.leaveType()
                ))
                .build();
        publisher.publishToCompanyLeaveRequests(event.companyId(), envelope);
        if (event.employeeUserId() != null) {
            publisher.publishToUser(event.employeeUserId(), envelope);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaveRequestRejected(LeaveRequestRejectedDomainEvent event) {
        RealtimeEventEnvelope envelope = RealtimeEventEnvelope.builder()
                .type(LeaveRequestEventType.LEAVE_REQUEST_REJECTED)
                .entity("leaveRequest")
                .entityId(event.leaveRequestId())
                .scopeId(event.companyId())
                .actorUserId(event.actorUserId())
                .payload(Map.of(
                        "leaveRequestId", event.leaveRequestId(),
                        "employeeId", event.employeeId(),
                        "leaveType", event.leaveType(),
                        "rejectionReason", event.rejectionReason() != null ? event.rejectionReason() : ""
                ))
                .build();
        publisher.publishToCompanyLeaveRequests(event.companyId(), envelope);
        if (event.employeeUserId() != null) {
            publisher.publishToUser(event.employeeUserId(), envelope);
        }
    }
}
