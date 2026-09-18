package com.clearing.netting.application;

import com.clearing.netting.domain.model.AuditAction;
import com.clearing.netting.domain.model.AuditEvent;
import com.clearing.netting.domain.model.AuditOutcome;
import com.clearing.netting.domain.port.out.AuditEventRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AuditApplicationService {

    private final AuditEventRepositoryPort auditRepository;
    private final ObjectMapper objectMapper;

    public AuditApplicationService(AuditEventRepositoryPort auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> list(AuditAction action, String actor) {
        return auditRepository.findByFilters(action, actor);
    }

    @Transactional
    public void record(
            AuditAction action,
            AuditOutcome outcome,
            String actor,
            String refId,
            String summary,
            Map<String, Object> detail) {
        String safeSummary = summary != null && summary.length() > 500 ? summary.substring(0, 500) : summary;
        auditRepository.save(AuditEvent.of(action, outcome, actor, refId, safeSummary, toJson(detail)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInNewTx(
            AuditAction action,
            AuditOutcome outcome,
            String actor,
            String refId,
            String summary,
            Map<String, Object> detail) {
        record(action, outcome, actor, refId, summary, detail);
    }

    private String toJson(Map<String, Object> detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception ex) {
            return String.valueOf(detail);
        }
    }
}
