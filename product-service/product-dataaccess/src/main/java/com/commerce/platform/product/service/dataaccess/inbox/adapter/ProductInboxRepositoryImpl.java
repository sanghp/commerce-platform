package com.commerce.platform.product.service.dataaccess.inbox.adapter;

import com.commerce.platform.product.service.dataaccess.inbox.mapper.ProductInboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.inbox.repository.ProductInboxJpaRepository;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import org.springframework.stereotype.Component;

@Component
public class ProductInboxRepositoryImpl implements ProductInboxRepository {

    private final ProductInboxJpaRepository inboxJpaRepository;
    private final ProductInboxDataAccessMapper inboxDataAccessMapper;

    public ProductInboxRepositoryImpl(ProductInboxJpaRepository inboxJpaRepository,
                                      ProductInboxDataAccessMapper inboxDataAccessMapper) {
        this.inboxJpaRepository = inboxJpaRepository;
        this.inboxDataAccessMapper = inboxDataAccessMapper;
    }

    @Override
    public ProductInboxMessage save(ProductInboxMessage inboxMessage) {
        return inboxDataAccessMapper
                .productInboxEntityToInboxMessage(inboxJpaRepository
                        .save(inboxDataAccessMapper
                                .productInboxMessageToInboxEntity(inboxMessage)));
    }
} 