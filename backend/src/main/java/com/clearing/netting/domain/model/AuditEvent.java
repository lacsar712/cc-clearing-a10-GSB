package com.clearing.netting.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AuditEvent {
    private final String eventId;
    private final AuditEventType eventType;
    private final String actor;
    private final String refId;
    private final String summary;
    private final String detail;
    private final Instant createdAt;

    public AuditEvent(
            String eventId,
            AuditEventType eventType,
            String actor,
            String refId,
            String summary,
            String detail,
            Instant createdAt) {
        this.eventId = Objects.requireNonNull(eventId);
        this.eventType = Objects.requireNonNull(eventType);
        this.actor = Objects.requireNonNull(actor);
        this.refId = refId;
        this.summary = Objects.requireNonNull(summary);
        this.detail = detail;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static AuditEvent of(
            AuditEventType eventType,
            String actor,
            String refId,
            String summary,
            String detail) {
        return new AuditEvent(
                UUID.randomUUID().toString(),
                eventType,
                actor,
                refId,
                truncate(summary, 500),
                truncate(detail, 2000),
                Instant.now());
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    public String getEventId() {
        return eventId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getRefId() {
        return refId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
