package com.worknest.realtime.publisher;

import com.worknest.realtime.event.RealtimeEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishToCompanySites(UUID companyId, RealtimeEventEnvelope event) {
        String destination = "/topic/companies/" + companyId + "/sites";
        send(destination, event);
    }

    public void publishToCompanySite(UUID companyId, UUID siteId, RealtimeEventEnvelope event) {
        String destination = "/topic/companies/" + companyId + "/sites/" + siteId;
        send(destination, event);
    }

    public void publishToCompanyAnnouncements(UUID companyId, RealtimeEventEnvelope event) {
        send("/topic/companies/" + companyId + "/announcements", event);
    }

    public void publishToCompanyLeaveRequests(UUID companyId, RealtimeEventEnvelope event) {
        send("/topic/companies/" + companyId + "/leave-requests", event);
    }

    public void publishToCompanyDepartments(UUID companyId, RealtimeEventEnvelope event) {
        send("/topic/companies/" + companyId + "/departments", event);
    }

    public void publishToCompanyEmployees(UUID companyId, RealtimeEventEnvelope event) {
        send("/topic/companies/" + companyId + "/employees", event);
    }

    public void publishToCompanyAttendance(UUID companyId, RealtimeEventEnvelope event) {
        send("/topic/companies/" + companyId + "/attendance", event);
    }

    public void publishToCompanySettings(UUID companyId, RealtimeEventEnvelope event) {
        send("/topic/companies/" + companyId + "/settings", event);
    }

    public void publishToUser(UUID userId, RealtimeEventEnvelope event) {
        send("/user/" + userId + "/queue/notifications", event);
    }

    private void send(String destination, RealtimeEventEnvelope event) {
        try {
            messagingTemplate.convertAndSend(destination, event);
            log.info("Realtime event published: type={} entityId={} scopeId={} destination={}",
                    event.type(), event.entityId(), event.scopeId(), destination);
        } catch (Exception e) {
            log.error("Failed to publish realtime event: type={} entityId={} destination={} error={}",
                    event.type(), event.entityId(), destination, e.getMessage());
        }
    }
}
