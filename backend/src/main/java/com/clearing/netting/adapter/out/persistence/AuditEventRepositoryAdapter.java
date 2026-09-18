package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.entity.AuditEventJpaEntity;
import com.clearing.netting.adapter.out.persistence.repo.AuditEventJpaRepository;
import com.clearing.netting.domain.model.AuditAction;
import com.clearing.netting.domain.model.AuditEvent;
import com.clearing.netting.domain.port.out.AuditEventRepositoryPort;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
    public List<AuditEvent> findByFilters(AuditAction action, String actor) {
        String actorFilter = actor == null || actor.isBlank() ? null : actor.trim();
        Specification<AuditEventJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (actorFilter != null) {
                predicates.add(cb.equal(root.get("actor"), actorFilter));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec).stream().map(PersistenceMapper::toDomain).collect(Collectors.toList());
    }
}
