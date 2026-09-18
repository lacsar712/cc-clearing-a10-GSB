package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.AuditEvent;
import com.clearing.netting.domain.model.AuditEventType;

import java.util.List;
import java.util.Optional;

public interface AuditEventRepositoryPort {
    AuditEvent save(AuditEvent event);

    Optional<AuditEvent> findById(String eventId);

    List<AuditEvent> findAllOrderByCreatedAtDesc();

    List<AuditEvent> findByEventTypeOrderByCreatedAtDesc(AuditEventType eventType);
}
