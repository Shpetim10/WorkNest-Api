package com.worknest.realtime.event;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEventEnvelope(
        String eventId,
        String type,
        String entity,
        String entityId,
        String scopeType,
        String scopeId,
        Long version,
        Instant occurredAt,
        String actorUserId,
        Object payload
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final String eventId = UUID.randomUUID().toString();
        private String type;
        private String entity;
        private String entityId;
        private final String scopeType = "company";
        private String scopeId;
        private Long version;
        private final Instant occurredAt = Instant.now();
        private String actorUserId;
        private Object payload;

        public Builder type(String type) { this.type = type; return this; }
        public Builder entity(String entity) { this.entity = entity; return this; }
        public Builder entityId(UUID entityId) { this.entityId = entityId.toString(); return this; }
        public Builder scopeId(UUID scopeId) { this.scopeId = scopeId.toString(); return this; }
        public Builder version(Long version) { this.version = version; return this; }
        public Builder actorUserId(UUID actorUserId) { this.actorUserId = actorUserId.toString(); return this; }
        public Builder payload(Object payload) { this.payload = payload; return this; }

        public RealtimeEventEnvelope build() {
            return new RealtimeEventEnvelope(eventId, type, entity, entityId, scopeType, scopeId, version, occurredAt, actorUserId, payload);
        }
    }
}
