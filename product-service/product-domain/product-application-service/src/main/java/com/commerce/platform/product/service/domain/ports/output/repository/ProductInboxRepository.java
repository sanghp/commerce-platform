package com.commerce.platform.product.service.domain.ports.output.repository;

import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;


public interface ProductInboxRepository {
    
    ProductInboxMessage save(ProductInboxMessage inboxMessage);
} 