package com.commerce.platform.order.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.dataaccess.jooq.tables.OrderOutbox;
import com.commerce.platform.order.service.dataaccess.outbox.entity.OrderOutboxEntity;
import com.commerce.platform.order.service.dataaccess.outbox.mapper.OrderOutboxDataAccessMapper;
import com.commerce.platform.order.service.dataaccess.outbox.repository.OrderOutboxJpaRepository;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import org.springframework.data.domain.PageRequest;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.commerce.platform.order.service.dataaccess.jooq.tables.OrderOutbox.ORDER_OUTBOX;
import static org.jooq.impl.DSL.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderOutboxRepositoryImpl implements OrderOutboxRepository {

    private final OrderOutboxJpaRepository orderOutboxJpaRepository;
    private final OrderOutboxDataAccessMapper orderOutboxDataAccessMapper;
    private final DSLContext dsl;

    public OrderOutboxRepositoryImpl(OrderOutboxJpaRepository orderOutboxJpaRepository,
                                    OrderOutboxDataAccessMapper orderOutboxDataAccessMapper,
                                    DSLContext dsl) {
        this.orderOutboxJpaRepository = orderOutboxJpaRepository;
        this.orderOutboxDataAccessMapper = orderOutboxDataAccessMapper;
        this.dsl = dsl;
    }

    @Override
    public OrderOutboxMessage save(OrderOutboxMessage orderOutboxMessage) {
        return orderOutboxDataAccessMapper
                .orderOutboxEntityToOrderOutboxMessage(orderOutboxJpaRepository
                        .save(orderOutboxDataAccessMapper
                                .orderOutboxMessageToOutboxEntity(orderOutboxMessage)));
    }

    @Override
    public List<OrderOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return orderOutboxJpaRepository.findByOutboxStatusOrderByCreatedAt(outboxStatus, PageRequest.of(0, limit))
                .stream()
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        var result = dsl.selectFrom(ORDER_OUTBOX)
            .where(ORDER_OUTBOX.OUTBOX_STATUS.eq(outboxStatus.name()))
            .orderBy(ORDER_OUTBOX.CREATED_AT)
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToOrderOutboxMessage);
    }

    @Override
    public List<OrderOutboxMessage> saveAll(List<OrderOutboxMessage> orderOutboxMessages) {
        List<OrderOutboxEntity> entities = orderOutboxMessages.stream()
                .map(orderOutboxDataAccessMapper::orderOutboxMessageToOutboxEntity)
                .collect(Collectors.toList());
        return orderOutboxJpaRepository.saveAll(entities).stream()
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus,
                                                                         ZonedDateTime fetchedAtBefore,
                                                                         int limit) {
        var result = dsl.selectFrom(ORDER_OUTBOX)
            .where(ORDER_OUTBOX.OUTBOX_STATUS.eq(outboxStatus.name())
                .and(ORDER_OUTBOX.FETCHED_AT.lt(fetchedAtBefore.toLocalDateTime())))
            .orderBy(ORDER_OUTBOX.CREATED_AT)
            .limit(limit)
            .fetch();
        
        return result.map(this::mapToOrderOutboxMessage);
    }

    @Override
    public int deleteByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        List<OrderOutboxEntity> entitiesToDelete = orderOutboxJpaRepository
                .findByOutboxStatusOrderByCreatedAt(outboxStatus, PageRequest.of(0, limit));
        int deletedCount = entitiesToDelete.size();
        if (deletedCount > 0) {
            orderOutboxJpaRepository.deleteAllInBatch(entitiesToDelete);
        }
        return deletedCount;
    }

    @Override
    public Optional<OrderOutboxMessage> findById(UUID id) {
        return orderOutboxJpaRepository.findById(id)
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage);
    }

    @Override
    @Transactional
    public int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt) {
        if (ids.isEmpty()) {
            return 0;
        }

        return dsl.update(ORDER_OUTBOX)
            .set(ORDER_OUTBOX.OUTBOX_STATUS, status.name())
            .set(ORDER_OUTBOX.FETCHED_AT, fetchedAt != null ? 
                fetchedAt.toLocalDateTime() : null)
            .where(ORDER_OUTBOX.ID.in(ids))
            .execute();
    }
    
    private OrderOutboxMessage mapToOrderOutboxMessage(Record record) {
        return OrderOutboxMessage.builder()
                .id(record.getValue(ORDER_OUTBOX.ID))
                .messageId(record.getValue(ORDER_OUTBOX.MESSAGE_ID))
                .sagaId(record.getValue(ORDER_OUTBOX.SAGA_ID))
                .type(record.getValue(ORDER_OUTBOX.TYPE))
                .payload(record.getValue(ORDER_OUTBOX.PAYLOAD).data())
                .outboxStatus(OutboxStatus.valueOf(record.getValue(ORDER_OUTBOX.OUTBOX_STATUS)))
                .createdAt(record.getValue(ORDER_OUTBOX.CREATED_AT).atZone(ZoneOffset.UTC))
                .processedAt(record.getValue(ORDER_OUTBOX.PROCESSED_AT) != null ? 
                    record.getValue(ORDER_OUTBOX.PROCESSED_AT).atZone(ZoneOffset.UTC) : null)
                .fetchedAt(record.getValue(ORDER_OUTBOX.FETCHED_AT) != null ? 
                    record.getValue(ORDER_OUTBOX.FETCHED_AT).atZone(ZoneOffset.UTC) : null)
                .version(record.getValue(ORDER_OUTBOX.VERSION))
                .build();
    }
    
}