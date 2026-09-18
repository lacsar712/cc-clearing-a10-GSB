package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.auth.AuthContext;
import com.clearing.netting.application.AuditApplicationService;
import com.clearing.netting.domain.model.AuditAction;
import com.clearing.netting.domain.model.AuditEvent;
import com.clearing.netting.domain.model.AuditOutcome;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-events")
public class AuditController {

    private final AuditApplicationService auditService;

    public AuditController(AuditApplicationService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditEventResponse> list(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String actor) {
        AuthContext.require();
        return auditService.list(action, actor).stream()
                .map(AuditEventResponse::from)
                .collect(Collectors.toList());
    }

    public record AuditEventResponse(
            String eventId,
            AuditAction action,
            AuditOutcome outcome,
            String actor,
            String refId,
            String summary,
            String detail,
            Instant createdAt) {
        static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(
                    e.getEventId(),
                    e.getAction(),
                    e.getOutcome(),
                    e.getActor(),
                    e.getRefId(),
                    e.getSummary(),
                    e.getDetail(),
                    e.getCreatedAt());
        }
    }
}
