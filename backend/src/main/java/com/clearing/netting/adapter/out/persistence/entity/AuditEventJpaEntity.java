package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.AuditEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEventJpaEntity {

    @Id
    @Column(length = 64)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditEventType eventType;

    @Column(nullable = false, length = 128)
    private String actor;

    @Column(length = 64)
    private String refId;

    @Column(nullable = false, length = 512)
    private String summary;

    @Column(length = 2048)
    private String detail;

    @Column(nullable = false)
    private Instant createdAt;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
