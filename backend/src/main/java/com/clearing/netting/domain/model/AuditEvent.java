package com.clearing.netting.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AuditEvent {
    private final String eventId;
    private final AuditAction action;
    private final AuditOutcome outcome;
    private final String actor;
    private final String refId;
    private final String summary;
    private final String detail;
    private final Instant createdAt;

    public AuditEvent(
            String eventId,
            AuditAction action,
            AuditOutcome outcome,
            String actor,
            String refId,
            String summary,
            String detail,
            Instant createdAt) {
        this.eventId = Objects.requireNonNull(eventId);
        this.action = Objects.requireNonNull(action);
        this.outcome = Objects.requireNonNull(outcome);
        this.actor = Objects.requireNonNull(actor);
        this.refId = refId;
        this.summary = Objects.requireNonNull(summary);
        this.detail = detail;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static AuditEvent of(
            AuditAction action,
            AuditOutcome outcome,
            String actor,
            String refId,
            String summary,
            String detail) {
        return new AuditEvent(
                UUID.randomUUID().toString(),
                action,
                outcome,
                actor,
                refId,
                summary,
                detail,
                Instant.now());
    }

    public String getEventId() {
        return eventId;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditOutcome getOutcome() {
        return outcome;
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
