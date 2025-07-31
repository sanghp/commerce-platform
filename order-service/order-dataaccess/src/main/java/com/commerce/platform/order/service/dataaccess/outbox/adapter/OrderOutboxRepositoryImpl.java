package com.commerce.platform.order.service.dataaccess.outbox.adapter;

import com.commerce.platform.order.service.dataaccess.outbox.entity.OrderOutboxEntity;
import com.commerce.platform.order.service.dataaccess.outbox.mapper.OrderOutboxDataAccessMapper;
import com.commerce.platform.order.service.dataaccess.outbox.repository.OrderOutboxJpaRepository;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderOutboxRepositoryImpl implements OrderOutboxRepository {

    private final OrderOutboxJpaRepository orderOutboxJpaRepository;
    private final OrderOutboxDataAccessMapper orderOutboxDataAccessMapper;

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
        return orderOutboxJpaRepository
                .findByOutboxStatusAndFetchedAtBeforeOrderByCreatedAt(outboxStatus, fetchedAtBefore, PageRequest.of(0, limit))
                .stream()
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OrderOutboxMessage> findByTypeAndSagaIdAndOutboxStatus(String type, UUID sagaId, OutboxStatus outboxStatus) {
        return orderOutboxJpaRepository
                .findByTypeAndSagaIdAndOutboxStatus(type, sagaId, outboxStatus)
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage);
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
}