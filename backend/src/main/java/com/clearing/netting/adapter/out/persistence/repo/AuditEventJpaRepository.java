package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.AuditEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventJpaRepository
        extends JpaRepository<AuditEventJpaEntity, String>, JpaSpecificationExecutor<AuditEventJpaEntity> {
}
