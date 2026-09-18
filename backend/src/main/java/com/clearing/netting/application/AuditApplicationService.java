package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.AuditEvent;
import com.clearing.netting.domain.model.AuditEventType;
import com.clearing.netting.domain.model.NetPosition;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.AuditEventRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
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
    public List<AuditEvent> list(AuditEventType eventType) {
        if (eventType == null) {
            return auditRepository.findAllOrderByCreatedAtDesc();
        }
        return auditRepository.findByEventTypeOrderByCreatedAtDesc(eventType);
    }

    @Transactional(readOnly = true)
    public AuditEvent get(String eventId) {
        return auditRepository.findById(eventId)
                .orElseThrow(() -> new DomainException("AUDIT_EVENT_NOT_FOUND", "audit event not found: " + eventId));
    }

    @Transactional
    public void recordNettingExecuted(
            String actor,
            NettingRun run,
            List<TradeObligation> obligations,
            List<NetPosition> positions) {
        BigDecimal sumNet = positions.stream()
                .map(NetPosition::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("runId", run.getRunId());
        detail.put("settleDate", run.getSettleDate());
        detail.put("currency", run.getCurrency());
        detail.put("status", run.getStatus().name());
        detail.put("obligationCount", obligations.size());
        detail.put("positionCount", positions.size());
        detail.put("sumNetAmount", sumNet);
        String summary = String.format(
                "执行轧差 %s %s：%s，义务 %d 笔，净头寸 %d 笔",
                run.getCurrency(), run.getSettleDate(), run.getStatus(), obligations.size(), positions.size());
        save(AuditEvent.of(AuditEventType.NETTING_EXECUTED, actor, run.getRunId(), summary, toJson(detail)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordNettingFailed(String actor, NettingRun run) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("runId", run.getRunId());
        detail.put("settleDate", run.getSettleDate());
        detail.put("currency", run.getCurrency());
        detail.put("status", run.getStatus().name());
        detail.put("failureReason", run.getFailureReason());
        String summary = String.format(
                "执行轧差 %s %s：FAILED（%s）",
                run.getCurrency(), run.getSettleDate(), run.getFailureReason());
        save(AuditEvent.of(AuditEventType.NETTING_EXECUTED, actor, run.getRunId(), summary, toJson(detail)));
    }

    @Transactional
    public void recordObligationCreated(String actor, TradeObligation obligation) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("obligationId", obligation.getObligationId());
        detail.put("payerMemberId", obligation.getPayerMemberId());
        detail.put("payeeMemberId", obligation.getPayeeMemberId());
        detail.put("currency", obligation.getCurrency());
        detail.put("amount", obligation.getAmount());
        detail.put("tradeDate", obligation.getTradeDate());
        detail.put("settleDate", obligation.getSettleDate());
        String summary = String.format(
                "新建义务 %s → %s %s %s（交割 %s）",
                obligation.getPayerMemberId(),
                obligation.getPayeeMemberId(),
                obligation.getCurrency(),
                obligation.getAmount(),
                obligation.getSettleDate());
        save(AuditEvent.of(
                AuditEventType.OBLIGATION_CREATED, actor, obligation.getObligationId(), summary, toJson(detail)));
    }

    private AuditEvent save(AuditEvent event) {
        return auditRepository.save(event);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception ex) {
            return detail.toString();
        }
    }
}
