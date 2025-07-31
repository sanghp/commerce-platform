package com.commerce.platform.product.service.domain.ports.output.repository;

import com.commerce.platform.domain.event.ServiceMessageType;

import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.outbox.OutboxStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductOutboxRepository {
    ProductOutboxMessage save(ProductOutboxMessage outboxMessage);
    
    List<ProductOutboxMessage> saveAll(List<ProductOutboxMessage> outboxMessages);

    Optional<List<ProductOutboxMessage>> findByTypeAndOutboxStatus(ServiceMessageType type,
                                                                  OutboxStatus outboxStatus);
    Optional<ProductOutboxMessage> findByTypeAndSagaId(ServiceMessageType type, UUID sagaId);
    Optional<ProductOutboxMessage> findBySagaId(UUID sagaId);
    void deleteByOutboxStatus(OutboxStatus outboxStatus);

    List<ProductOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit);
    
    Optional<ProductOutboxMessage> findById(UUID id);
} 