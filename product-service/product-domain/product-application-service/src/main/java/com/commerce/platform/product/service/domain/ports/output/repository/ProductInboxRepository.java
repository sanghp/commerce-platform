package com.commerce.platform.product.service.domain.ports.output.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;

import java.util.Optional;
import java.util.UUID;

public interface ProductInboxRepository {
    
    ProductInboxMessage save(ProductInboxMessage inboxMessage);
    
    Optional<ProductInboxMessage> findBySagaIdAndEventType(UUID sagaId, ServiceMessageType eventType);
} 