package com.commerce.platform.order.service.domain.ports.output.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderInboxRepository {
    OrderInboxMessage save(OrderInboxMessage orderInboxMessage);
    List<OrderInboxMessage> saveAll(List<OrderInboxMessage> orderInboxMessages);
    Optional<OrderInboxMessage> findBySagaIdAndEventType(UUID sagaId, ServiceMessageType eventType);
    List<OrderInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit); // FOR UPDATE SKIP LOCKED
    List<OrderInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit); // FOR UPDATE SKIP LOCKED
}