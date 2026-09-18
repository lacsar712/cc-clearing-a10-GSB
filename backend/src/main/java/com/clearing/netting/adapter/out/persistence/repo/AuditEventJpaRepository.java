package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.AuditEventJpaEntity;
import com.clearing.netting.domain.model.AuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventJpaEntity, String> {
    List<AuditEventJpaEntity> findAllByOrderByCreatedAtDesc();

    List<AuditEventJpaEntity> findByEventTypeOrderByCreatedAtDesc(AuditEventType eventType);
}
