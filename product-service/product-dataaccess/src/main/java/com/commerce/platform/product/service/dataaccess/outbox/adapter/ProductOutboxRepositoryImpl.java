package com.commerce.platform.product.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.outbox.mapper.ProductOutboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.outbox.repository.ProductOutboxJpaRepository;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.jooq.impl.DSL.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductOutboxRepositoryImpl implements ProductOutboxRepository {

    private final ProductOutboxJpaRepository outboxJpaRepository;
    private final ProductOutboxDataAccessMapper outboxDataAccessMapper;
    private final DSLContext dsl;

    public ProductOutboxRepositoryImpl(ProductOutboxJpaRepository outboxJpaRepository,
                                      ProductOutboxDataAccessMapper outboxDataAccessMapper,
                                      DSLContext dsl) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.outboxDataAccessMapper = outboxDataAccessMapper;
        this.dsl = dsl;
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
        return outboxJpaRepository.findByOutboxStatus(outboxStatus, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        var result = dsl.selectFrom(table("product_outbox"))
            .where(field("outbox_status").eq(outboxStatus.name()))
            .orderBy(field("created_at"))
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToProductOutboxMessage);
    }
    
    @Override
    public List<ProductOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        var result = dsl.selectFrom(table("product_outbox"))
            .where(field("outbox_status").eq(outboxStatus.name())
                .and(field("fetched_at").lt(java.sql.Timestamp.from(fetchedAtBefore.toInstant()))))
            .orderBy(field("created_at"))
            .limit(limit)
            .fetch();
        
        return result.map(this::mapToProductOutboxMessage);
    }
    
    @Override
    public Optional<ProductOutboxMessage> findById(UUID id) {
        return outboxJpaRepository.findById(id)
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage);
    }
    
    @Override
    @Transactional
    public int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt) {
        if (ids.isEmpty()) {
            return 0;
        }

        int totalUpdated = 0;
        for (UUID id : ids) {
            int updated = dsl.update(table("product_outbox"))
                .set(field("outbox_status"), status.name())
                .set(field("fetched_at"), fetchedAt != null ? 
                    java.sql.Timestamp.from(fetchedAt.toInstant()) : null)
                .where(field("id").eq(id))
                .execute();
            totalUpdated += updated;
        }

        return totalUpdated;
    }
    
    private ProductOutboxMessage mapToProductOutboxMessage(Record record) {
        return ProductOutboxMessage.builder()
                .id(record.getValue("id", UUID.class))
                .messageId(record.getValue("message_id", UUID.class))
                .sagaId(record.getValue("saga_id", UUID.class))
                .type(ServiceMessageType.valueOf(record.getValue("type", String.class)))
                .payload(record.getValue("payload", String.class))
                .outboxStatus(OutboxStatus.valueOf(record.getValue("outbox_status", String.class)))
                .createdAt(ZonedDateTime.ofInstant(
                    record.getValue("created_at", java.sql.Timestamp.class).toInstant(), 
                    ZoneOffset.UTC))
                .processedAt(record.getValue("processed_at", java.sql.Timestamp.class) != null ? 
                    ZonedDateTime.ofInstant(
                        record.getValue("processed_at", java.sql.Timestamp.class).toInstant(), 
                        ZoneOffset.UTC) : null)
                .fetchedAt(record.getValue("fetched_at", java.sql.Timestamp.class) != null ? 
                    ZonedDateTime.ofInstant(
                        record.getValue("fetched_at", java.sql.Timestamp.class).toInstant(), 
                        ZoneOffset.UTC) : null)
                .version(record.getValue("version", Integer.class))
                .build();
    }
} 