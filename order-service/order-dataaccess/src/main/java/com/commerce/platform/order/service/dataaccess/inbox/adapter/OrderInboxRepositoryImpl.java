package com.commerce.platform.order.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.dataaccess.inbox.mapper.OrderInboxDataAccessMapper;
import com.commerce.platform.order.service.dataaccess.inbox.repository.OrderInboxJpaRepository;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderInboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import org.jooq.JSON;

import static com.commerce.platform.order.service.dataaccess.jooq.tables.OrderInbox.ORDER_INBOX;
import static org.jooq.impl.DSL.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderInboxRepositoryImpl implements OrderInboxRepository {

    private final OrderInboxJpaRepository orderInboxJpaRepository;
    private final OrderInboxDataAccessMapper orderInboxDataAccessMapper;
    private final DSLContext dsl;

    public OrderInboxRepositoryImpl(OrderInboxJpaRepository orderInboxJpaRepository,
                                    OrderInboxDataAccessMapper orderInboxDataAccessMapper,
                                    DSLContext dsl) {
        this.orderInboxJpaRepository = orderInboxJpaRepository;
        this.orderInboxDataAccessMapper = orderInboxDataAccessMapper;
        this.dsl = dsl;
    }

    @Override
    public OrderInboxMessage save(OrderInboxMessage orderInboxMessage) {
        return orderInboxDataAccessMapper.orderInboxEntityToOrderInboxMessage(
                orderInboxJpaRepository.save(
                        orderInboxDataAccessMapper.orderInboxMessageToOrderInboxEntity(orderInboxMessage)
                )
        );
    }
    
    @Override
    public List<OrderInboxMessage> saveAll(List<OrderInboxMessage> orderInboxMessages) {
        if (orderInboxMessages.isEmpty()) {
            return orderInboxMessages;
        }
        
        int insertedCount = 0;
        for (OrderInboxMessage message : orderInboxMessages) {
            int result = dsl.insertInto(ORDER_INBOX)
                .columns(
                    ORDER_INBOX.ID,
                    ORDER_INBOX.MESSAGE_ID,
                    ORDER_INBOX.SAGA_ID,
                    ORDER_INBOX.TYPE,
                    ORDER_INBOX.PAYLOAD,
                    ORDER_INBOX.STATUS,
                    ORDER_INBOX.RECEIVED_AT,
                    ORDER_INBOX.RETRY_COUNT
                )
                .values(
                    message.getId(),
                    message.getMessageId(),
                    message.getSagaId(),
                    message.getType().name(),
                    JSON.json(message.getPayload()),
                    message.getStatus().name(),
                    message.getReceivedAt().toLocalDateTime(),
                    message.getRetryCount()
                )
                .onDuplicateKeyIgnore()
                .execute();
            insertedCount += result;
        }
        
        log.info("Inserted {} new messages out of {} total messages to inbox", insertedCount, orderInboxMessages.size());
        
        return orderInboxMessages;
    }

    @Override
    public Optional<OrderInboxMessage> findByMessageId(UUID messageId) {
        return orderInboxJpaRepository.findByMessageId(messageId)
                .map(orderInboxDataAccessMapper::orderInboxEntityToOrderInboxMessage);
    }
    
    @Override
    public List<OrderInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit) {
        var result = dsl.selectFrom(ORDER_INBOX)
            .where(ORDER_INBOX.STATUS.eq(status.name()))
            .orderBy(ORDER_INBOX.RECEIVED_AT)
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToOrderInboxMessage);
    }
    
    @Override
    public List<OrderInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        var result = dsl.selectFrom(ORDER_INBOX)
            .where(ORDER_INBOX.STATUS.eq(status.name())
                .and(ORDER_INBOX.RETRY_COUNT.lt(maxRetryCount)))
            .orderBy(ORDER_INBOX.RECEIVED_AT)
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToOrderInboxMessage);
    }
    
    private OrderInboxMessage mapToOrderInboxMessage(Record record) {
        return OrderInboxMessage.builder()
                .id(record.getValue(ORDER_INBOX.ID))
                .messageId(record.getValue(ORDER_INBOX.MESSAGE_ID))
                .sagaId(record.getValue(ORDER_INBOX.SAGA_ID))
                .type(ServiceMessageType.valueOf(record.getValue(ORDER_INBOX.TYPE)))
                .payload(record.getValue(ORDER_INBOX.PAYLOAD).data())
                .status(InboxStatus.valueOf(record.getValue(ORDER_INBOX.STATUS)))
                .receivedAt(record.getValue(ORDER_INBOX.RECEIVED_AT).atZone(ZoneOffset.UTC))
                .processedAt(record.getValue(ORDER_INBOX.PROCESSED_AT) != null ? 
                    record.getValue(ORDER_INBOX.PROCESSED_AT).atZone(ZoneOffset.UTC) : null)
                .retryCount(record.getValue(ORDER_INBOX.RETRY_COUNT))
                .errorMessage(record.getValue(ORDER_INBOX.ERROR_MESSAGE))
                .build();
    }
    
}