package com.commerce.platform.product.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.outbox.mapper.ProductOutboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.outbox.repository.ProductOutboxJpaRepository;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.commerce.platform.product.service.dataaccess.jooq.tables.ProductOutbox.PRODUCT_OUTBOX;
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
        var result = dsl.selectFrom(PRODUCT_OUTBOX)
            .where(PRODUCT_OUTBOX.OUTBOX_STATUS.eq(outboxStatus.name()))
            .orderBy(PRODUCT_OUTBOX.CREATED_AT)
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToProductOutboxMessage);
    }
    
    @Override
    public List<ProductOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        var result = dsl.selectFrom(PRODUCT_OUTBOX)
            .where(PRODUCT_OUTBOX.OUTBOX_STATUS.eq(outboxStatus.name())
                .and(PRODUCT_OUTBOX.FETCHED_AT.lt(fetchedAtBefore.toLocalDateTime())))
            .orderBy(PRODUCT_OUTBOX.CREATED_AT)
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

        return dsl.update(PRODUCT_OUTBOX)
            .set(PRODUCT_OUTBOX.OUTBOX_STATUS, status.name())
            .set(PRODUCT_OUTBOX.FETCHED_AT, fetchedAt != null ? 
                fetchedAt.toLocalDateTime() : null)
            .where(PRODUCT_OUTBOX.ID.in(ids))
            .execute();
    }
    
    private ProductOutboxMessage mapToProductOutboxMessage(Record record) {
        return ProductOutboxMessage.builder()
                .id(record.getValue(PRODUCT_OUTBOX.ID))
                .messageId(record.getValue(PRODUCT_OUTBOX.MESSAGE_ID))
                .sagaId(record.getValue(PRODUCT_OUTBOX.SAGA_ID))
                .type(ServiceMessageType.valueOf(record.getValue(PRODUCT_OUTBOX.TYPE)))
                .payload(record.getValue(PRODUCT_OUTBOX.PAYLOAD).data())
                .outboxStatus(OutboxStatus.valueOf(record.getValue(PRODUCT_OUTBOX.OUTBOX_STATUS)))
                .createdAt(record.getValue(PRODUCT_OUTBOX.CREATED_AT).atZone(ZoneOffset.UTC))
                .processedAt(record.getValue(PRODUCT_OUTBOX.PROCESSED_AT) != null ? 
                    record.getValue(PRODUCT_OUTBOX.PROCESSED_AT).atZone(ZoneOffset.UTC) : null)
                .fetchedAt(record.getValue(PRODUCT_OUTBOX.FETCHED_AT) != null ? 
                    record.getValue(PRODUCT_OUTBOX.FETCHED_AT).atZone(ZoneOffset.UTC) : null)
                .version(record.getValue(PRODUCT_OUTBOX.VERSION))
                .build();
    }
}