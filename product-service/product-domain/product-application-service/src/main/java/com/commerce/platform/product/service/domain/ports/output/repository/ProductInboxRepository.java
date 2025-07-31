package com.commerce.platform.product.service.domain.ports.output.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductInboxRepository {
    ProductInboxMessage save(ProductInboxMessage productInboxMessage);
    List<ProductInboxMessage> saveAll(List<ProductInboxMessage> productInboxMessages);
    Optional<ProductInboxMessage> findBySagaIdAndType(UUID sagaId, ServiceMessageType type);
    List<ProductInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit); // FOR UPDATE SKIP LOCKED
    List<ProductInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit); // FOR UPDATE SKIP LOCKED
} 