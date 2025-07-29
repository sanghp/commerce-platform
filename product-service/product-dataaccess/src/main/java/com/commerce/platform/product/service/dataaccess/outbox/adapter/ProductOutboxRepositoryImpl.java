package com.commerce.platform.product.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;

import com.commerce.platform.product.service.dataaccess.outbox.mapper.ProductOutboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.outbox.repository.ProductOutboxJpaRepository;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductOutboxRepositoryImpl implements ProductOutboxRepository {

    private final ProductOutboxJpaRepository outboxJpaRepository;
    private final ProductOutboxDataAccessMapper outboxDataAccessMapper;

    public ProductOutboxRepositoryImpl(ProductOutboxJpaRepository outboxJpaRepository,
                                                  ProductOutboxDataAccessMapper outboxDataAccessMapper) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.outboxDataAccessMapper = outboxDataAccessMapper;
    }

    @Override
    public ProductOutboxMessage save(ProductOutboxMessage outboxMessage) {
        return outboxDataAccessMapper
                .productOutboxEntityToOutboxMessage(outboxJpaRepository
                        .save(outboxDataAccessMapper
                                .productOutboxMessageToOutboxEntity(outboxMessage)));
    }

    @Override
    public Optional<List<ProductOutboxMessage>> findByTypeAndOutboxStatus(ServiceMessageType type, OutboxStatus outboxStatus) {
        return Optional.of(outboxJpaRepository.findByTypeAndOutboxStatus(type, outboxStatus).orElse(List.of())
                .stream()
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage)
                .collect(Collectors.toList()));
    }

    @Override
    public Optional<ProductOutboxMessage> findByTypeAndSagaId(ServiceMessageType type, UUID sagaId) {
        return outboxJpaRepository.findByTypeAndSagaId(type, sagaId)
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage);
    }

    @Override
    public Optional<ProductOutboxMessage> findBySagaId(UUID sagaId) {
        return outboxJpaRepository.findBySagaId(sagaId)
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage);
    }

    @Override
    public void deleteByOutboxStatus(OutboxStatus outboxStatus) {
        outboxJpaRepository.deleteByOutboxStatus(outboxStatus);
    }

    @Override
    public Optional<List<ProductOutboxMessage>> findByOutboxStatus(OutboxStatus outboxStatus) {
        return outboxJpaRepository.findByOutboxStatus(outboxStatus)
                .map(outboxEntities -> outboxEntities.stream()
                        .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage)
                        .collect(Collectors.toList()));
    }
} 