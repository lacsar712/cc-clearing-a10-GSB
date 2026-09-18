package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.AuditAction;
import com.clearing.netting.domain.model.AuditEvent;

import java.util.List;

public interface AuditEventRepositoryPort {
    AuditEvent save(AuditEvent event);

    List<AuditEvent> findByFilters(AuditAction action, String actor);
}
