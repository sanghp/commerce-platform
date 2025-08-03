package com.commerce.platform.product.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;

import com.commerce.platform.product.service.dataaccess.outbox.mapper.ProductOutboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.outbox.repository.ProductOutboxJpaRepository;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import org.springframework.data.domain.PageRequest;
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
    public List<ProductOutboxMessage> saveAll(List<ProductOutboxMessage> outboxMessages) {
        return outboxJpaRepository
                .saveAll(outboxMessages.stream()
                        .map(outboxDataAccessMapper::productOutboxMessageToOutboxEntity)
                        .collect(Collectors.toList()))
                .stream()
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByOutboxStatus(OutboxStatus outboxStatus) {
        outboxJpaRepository.deleteByOutboxStatus(outboxStatus);
    }

    @Override
    public List<ProductOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return outboxJpaRepository.findByOutboxStatus(outboxStatus, PageRequest.of(0, limit))
                .stream()
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<ProductOutboxMessage> findById(UUID id) {
        return outboxJpaRepository.findById(id)
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage);
    }
} 