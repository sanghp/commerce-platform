package com.commerce.platform.order.service.domain.ports.output.repository;

import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.outbox.OutboxStatus;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderOutboxRepository {

    OrderOutboxMessage save(OrderOutboxMessage orderOutboxMessage);
    
    List<OrderOutboxMessage> saveAll(List<OrderOutboxMessage> orderOutboxMessages);
    
    List<OrderOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit);

    List<OrderOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit);
    
    List<OrderOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus, 
                                                                  ZonedDateTime fetchedAtBefore, 
                                                                  int limit);

    int deleteByOutboxStatus(OutboxStatus outboxStatus, int limit);
    
    Optional<OrderOutboxMessage> findById(UUID id);
    
    int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt);
}