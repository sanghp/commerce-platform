package com.commerce.platform.order.service.domain.ports.output.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderInboxRepository {
    OrderInboxMessage save(OrderInboxMessage orderInboxMessage);
    List<OrderInboxMessage> saveAll(List<OrderInboxMessage> orderInboxMessages);
    Optional<OrderInboxMessage> findByMessageId(UUID messageId);
    List<OrderInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit);
    List<OrderInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit);
}