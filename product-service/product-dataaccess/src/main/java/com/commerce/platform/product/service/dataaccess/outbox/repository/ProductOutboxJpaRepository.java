package com.commerce.platform.product.service.dataaccess.outbox.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.outbox.entity.ProductOutboxEntity;
import com.commerce.platform.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductOutboxJpaRepository extends JpaRepository<ProductOutboxEntity, UUID> {

    Optional<List<ProductOutboxEntity>> findByTypeAndOutboxStatus(ServiceMessageType type,
                                                                  OutboxStatus outboxStatus);

    Optional<ProductOutboxEntity> findByTypeAndSagaId(ServiceMessageType type, UUID sagaId);

    Optional<ProductOutboxEntity> findBySagaId(UUID sagaId);

    void deleteByOutboxStatus(OutboxStatus outboxStatus);

    Optional<List<ProductOutboxEntity>> findByOutboxStatus(OutboxStatus outboxStatus);
    
    List<ProductOutboxEntity> findByOutboxStatus(OutboxStatus outboxStatus, Pageable pageable);
}