package com.commerce.platform.order.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.dataaccess.inbox.mapper.OrderInboxDataAccessMapper;
import com.commerce.platform.order.service.dataaccess.inbox.repository.OrderInboxJpaRepository;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderInboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderInboxRepositoryImpl implements OrderInboxRepository {

    private final OrderInboxJpaRepository orderInboxJpaRepository;
    private final OrderInboxDataAccessMapper orderInboxDataAccessMapper;

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
        return orderInboxJpaRepository.saveAll(
                orderInboxMessages.stream()
                        .map(orderInboxDataAccessMapper::orderInboxMessageToOrderInboxEntity)
                        .collect(Collectors.toList())
        ).stream()
                .map(orderInboxDataAccessMapper::orderInboxEntityToOrderInboxMessage)
                .collect(Collectors.toList());
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