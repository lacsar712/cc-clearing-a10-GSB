package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.AuditAction;
import com.clearing.netting.domain.model.AuditOutcome;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.NetPosition;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.NetPositionRepositoryPort;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import com.clearing.netting.domain.service.MultilateralNettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NettingApplicationService {

    private final NettingRunRepositoryPort runRepository;
    private final ObligationRepositoryPort obligationRepository;
    private final MemberRepositoryPort memberRepository;
    private final NetPositionRepositoryPort positionRepository;
    private final NettingRunStatusService statusService;
    private final AuditApplicationService auditService;
    private final MultilateralNettingService nettingService;

    public NettingApplicationService(
            NettingRunRepositoryPort runRepository,
            ObligationRepositoryPort obligationRepository,
            MemberRepositoryPort memberRepository,
            NetPositionRepositoryPort positionRepository,
            NettingRunStatusService statusService,
            AuditApplicationService auditService) {
        this.runRepository = runRepository;
        this.obligationRepository = obligationRepository;
        this.memberRepository = memberRepository;
        this.positionRepository = positionRepository;
        this.statusService = statusService;
        this.auditService = auditService;
        this.nettingService = new MultilateralNettingService();
    }

    @Transactional(readOnly = true)
    public List<NettingRun> listRuns() {
        return runRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public NettingRun getRun(String runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new DomainException("RUN_NOT_FOUND", "netting run not found: " + runId));
    }

    @Transactional(readOnly = true)
    public List<NetPosition> getPositions(String runId) {
        getRun(runId);
        return positionRepository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<TradeObligation> getRunObligations(String runId) {
        getRun(runId);
        return obligationRepository.findByNettingRunId(runId);
    }

    @Transactional
    public NettingRunResult execute(LocalDate settleDate, String currency, String actor) {
        if (settleDate == null) {
            throw new DomainException("INVALID_DATE", "settleDate is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new DomainException("INVALID_CURRENCY", "currency is required");
        }
        String ccy = currency.trim().toUpperCase();

        NettingRun run = NettingRun.create(settleDate, ccy);
        run.markRunning();
        run = statusService.saveInNewTx(run);

        try {
            List<TradeObligation> opens = obligationRepository.findOpenBySettleDateAndCurrency(settleDate, ccy);
            Set<String> memberIds = new HashSet<>();
            for (TradeObligation o : opens) {
                memberIds.add(o.getPayerMemberId());
                memberIds.add(o.getPayeeMemberId());
            }
            Map<String, Member> members = new HashMap<>();
            for (Member m : memberRepository.findByIds(memberIds)) {
                members.put(m.getMemberId(), m);
            }

            List<NetPosition> positions = nettingService.net(run.getRunId(), ccy, opens, members);

            for (TradeObligation o : opens) {
                o.markNetted(run.getRunId());
            }
            obligationRepository.saveAll(opens);
            positionRepository.saveAll(positions);

            run.markCompleted();
            run = runRepository.save(run);
            auditNetting(run, positions, opens, actor);
            return new NettingRunResult(run, positions, opens);
        } catch (DomainException ex) {
            run.markFailed(ex.getMessage());
            statusService.saveInNewTx(run);
            auditNettingFailure(run, actor, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            run.markFailed(ex.getMessage() == null ? "unexpected error" : ex.getMessage());
            statusService.saveInNewTx(run);
            auditNettingFailure(run, actor, run.getFailureReason());
            throw new DomainException("NETTING_FAILED", ex.getMessage());
        }
    }

    private void auditNetting(
            NettingRun run, List<NetPosition> positions, List<TradeObligation> opens, String actor) {
        BigDecimal sumNet = positions.stream()
                .map(NetPosition::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("runId", run.getRunId());
        detail.put("settleDate", run.getSettleDate().toString());
        detail.put("currency", run.getCurrency());
        detail.put("status", run.getStatus().name());
        detail.put("obligationCount", opens.size());
        detail.put("positionCount", positions.size());
        detail.put("sumNetAmount", sumNet.toPlainString());
        detail.put("positions", positions.stream()
                .map(p -> Map.of(
                        "memberId", p.getMemberId(),
                        "netAmount", p.getNetAmount().toPlainString()))
                .toList());
        String summary = String.format(
                "轧差完成 %s %s：义务 %d 笔，净头寸 %d 笔，Σnet=%s",
                run.getCurrency(), run.getSettleDate(), opens.size(), positions.size(), sumNet.toPlainString());
        auditService.record(
                AuditAction.NETTING_EXECUTE, AuditOutcome.SUCCESS, actor, run.getRunId(), summary, detail);
    }

    private void auditNettingFailure(NettingRun run, String actor, String reason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("runId", run.getRunId());
        detail.put("settleDate", run.getSettleDate().toString());
        detail.put("currency", run.getCurrency());
        detail.put("status", NettingRunStatus.FAILED.name());
        detail.put("error", reason);
        String summary = String.format(
                "轧差失败 %s %s：%s", run.getCurrency(), run.getSettleDate(), reason);
        // 失败审计必须独立于已回滚的业务事务落库；审计自身异常不能掩盖原始错误
        try {
            auditService.recordInNewTx(
                    AuditAction.NETTING_EXECUTE, AuditOutcome.FAILED, actor, run.getRunId(), summary, detail);
        } catch (RuntimeException ignored) {
            // keep the original netting failure as the propagated error
        }
    }

    @Transactional
    public NettingRun settle(String runId) {
        NettingRun run = getRun(runId);
        if (run.getStatus() != NettingRunStatus.COMPLETED) {
            throw new DomainException("INVALID_STATE", "only COMPLETED runs can be settled");
        }
        List<TradeObligation> obligations = obligationRepository.findByNettingRunId(runId);
        if (obligations.isEmpty()) {
            throw new DomainException("NO_OBLIGATIONS", "no obligations linked to run");
        }
        for (TradeObligation o : obligations) {
            if (o.getStatus() == ObligationStatus.NETTED) {
                o.markSettled();
            } else if (o.getStatus() != ObligationStatus.SETTLED) {
                throw new DomainException("INVALID_STATE", "obligation not NETTED: " + o.getObligationId());
            }
        }
        obligationRepository.saveAll(obligations);
        return run;
    }

    public record NettingRunResult(NettingRun run, List<NetPosition> positions, List<TradeObligation> obligations) {
    }
}
