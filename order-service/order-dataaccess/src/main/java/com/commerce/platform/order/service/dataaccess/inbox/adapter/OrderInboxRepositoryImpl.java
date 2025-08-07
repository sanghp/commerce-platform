package com.commerce.platform.order.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.dataaccess.inbox.mapper.OrderInboxDataAccessMapper;
import com.commerce.platform.order.service.dataaccess.inbox.repository.OrderInboxJpaRepository;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderInboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderInboxRepositoryImpl implements OrderInboxRepository {

    private final OrderInboxJpaRepository orderInboxJpaRepository;
    private final OrderInboxDataAccessMapper orderInboxDataAccessMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public OrderInboxRepositoryImpl(OrderInboxJpaRepository orderInboxJpaRepository,
                                    OrderInboxDataAccessMapper orderInboxDataAccessMapper) {
        this.orderInboxJpaRepository = orderInboxJpaRepository;
        this.orderInboxDataAccessMapper = orderInboxDataAccessMapper;
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
        
        String sql = "INSERT IGNORE INTO order_inbox (id, message_id, saga_id, type, payload, status, received_at, retry_count) " +
                     "VALUES (UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')), ?, ?, ?, ?, ?)";
        
        int insertedCount = 0;
        for (OrderInboxMessage message : orderInboxMessages) {
            int result = jdbcTemplate.update(sql,
                message.getId().toString(),
                message.getMessageId().toString(),
                message.getSagaId().toString(),
                message.getType().name(),
                message.getPayload(),
                message.getStatus().name(),
                Timestamp.from(message.getReceivedAt().toInstant()),
                message.getRetryCount()
            );
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
        return orderInboxJpaRepository.findByStatusOrderByReceivedAt(status, PageRequest.of(0, limit))
                .stream()
                .map(orderInboxDataAccessMapper::orderInboxEntityToOrderInboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrderInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        return orderInboxJpaRepository.findByStatusAndRetryCountLessThanOrderByReceivedAt(
                        status, maxRetryCount, PageRequest.of(0, limit))
                .stream()
                .map(orderInboxDataAccessMapper::orderInboxEntityToOrderInboxMessage)
                .collect(Collectors.toList());
    }
}