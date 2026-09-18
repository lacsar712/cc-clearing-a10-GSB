package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.repo.AuditEventJpaRepository;
import com.clearing.netting.domain.model.AuditEvent;
import com.clearing.netting.domain.model.AuditEventType;
import com.clearing.netting.domain.port.out.AuditEventRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AuditEventRepositoryAdapter implements AuditEventRepositoryPort {

    private final AuditEventJpaRepository repository;

    public AuditEventRepositoryAdapter(AuditEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        return PersistenceMapper.toDomain(repository.save(PersistenceMapper.toEntity(event)));
    }

    @Override
    public Optional<AuditEvent> findById(String eventId) {
        return repository.findById(eventId).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<AuditEvent> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(PersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEvent> findByEventTypeOrderByCreatedAtDesc(AuditEventType eventType) {
        return repository.findByEventTypeOrderByCreatedAtDesc(eventType).stream()
                .map(PersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
