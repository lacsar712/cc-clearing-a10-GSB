package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.AuditAction;
import com.clearing.netting.domain.model.AuditOutcome;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ObligationApplicationService {

    private final ObligationRepositoryPort obligationRepository;
    private final MemberRepositoryPort memberRepository;
    private final AuditApplicationService auditService;

    public ObligationApplicationService(
            ObligationRepositoryPort obligationRepository,
            MemberRepositoryPort memberRepository,
            AuditApplicationService auditService) {
        this.obligationRepository = obligationRepository;
        this.memberRepository = memberRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TradeObligation> list(String currency, LocalDate settleDate, ObligationStatus status) {
        return obligationRepository.findByFilters(currency, settleDate, status);
    }

    @Transactional
    public TradeObligation create(
            String payerMemberId,
            String payeeMemberId,
            String currency,
            BigDecimal amount,
            LocalDate tradeDate,
            LocalDate settleDate,
            String actor) {
        validateMember(payerMemberId);
        validateMember(payeeMemberId);
        TradeObligation obligation = TradeObligation.open(
                payerMemberId, payeeMemberId, currency, amount, tradeDate, settleDate);
        TradeObligation saved = obligationRepository.save(obligation);
        audit(saved, actor);
        return saved;
    }

    private void audit(TradeObligation o, String actor) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("obligationId", o.getObligationId());
        detail.put("payerMemberId", o.getPayerMemberId());
        detail.put("payeeMemberId", o.getPayeeMemberId());
        detail.put("currency", o.getCurrency());
        detail.put("amount", o.getAmount().toPlainString());
        detail.put("tradeDate", o.getTradeDate().toString());
        detail.put("settleDate", o.getSettleDate().toString());
        detail.put("status", o.getStatus().name());
        String summary = String.format(
                "新建义务 %s → %s %s %s",
                o.getPayerMemberId(), o.getPayeeMemberId(), o.getAmount().toPlainString(), o.getCurrency());
        auditService.record(
                AuditAction.OBLIGATION_CREATE, AuditOutcome.SUCCESS, actor, o.getObligationId(), summary, detail);
    }

    private void validateMember(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new DomainException("MEMBER_NOT_FOUND", "member not found: " + memberId));
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new DomainException("SUSPENDED_MEMBER", "cannot create obligation for suspended member: " + memberId);
        }
    }
}
