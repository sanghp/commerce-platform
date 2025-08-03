package com.commerce.platform.product.service.domain.ports.output.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductInboxRepository {
    ProductInboxMessage save(ProductInboxMessage productInboxMessage);
    List<ProductInboxMessage> saveAll(List<ProductInboxMessage> productInboxMessages);
    Optional<ProductInboxMessage> findByMessageId(UUID messageId);
    List<ProductInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit);
    List<ProductInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit);
} 