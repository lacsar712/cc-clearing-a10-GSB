package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.auth.AuthContext;
import com.clearing.netting.application.AuditApplicationService;
import com.clearing.netting.domain.model.AuditEvent;
import com.clearing.netting.domain.model.AuditEventType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public List<AuditEventResponse> list(@RequestParam(required = false) AuditEventType eventType) {
        AuthContext.require();
        return auditService.list(eventType).stream()
                .map(AuditEventResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public AuditEventResponse get(@PathVariable("id") String id) {
        AuthContext.require();
        return AuditEventResponse.from(auditService.get(id));
    }

    public record AuditEventResponse(
            String eventId,
            AuditEventType eventType,
            String actor,
            String refId,
            String summary,
            String detail,
            Instant createdAt) {
        static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(
                    e.getEventId(),
                    e.getEventType(),
                    e.getActor(),
                    e.getRefId(),
                    e.getSummary(),
                    e.getDetail(),
                    e.getCreatedAt());
        }
    }
}
