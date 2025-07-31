package com.commerce.platform.product.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.inbox.mapper.ProductInboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.inbox.repository.ProductInboxJpaRepository;
import com.commerce.platform.product.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductInboxRepositoryImpl implements ProductInboxRepository {

    private final ProductInboxJpaRepository productInboxJpaRepository;
    private final ProductInboxDataAccessMapper productInboxDataAccessMapper;

    public ProductInboxRepositoryImpl(ProductInboxJpaRepository productInboxJpaRepository,
                                      ProductInboxDataAccessMapper productInboxDataAccessMapper) {
        this.productInboxJpaRepository = productInboxJpaRepository;
        this.productInboxDataAccessMapper = productInboxDataAccessMapper;
    }

    @Override
    public ProductInboxMessage save(ProductInboxMessage productInboxMessage) {
        return productInboxDataAccessMapper.productInboxEntityToProductInboxMessage(
                productInboxJpaRepository.save(
                        productInboxDataAccessMapper.productInboxMessageToProductInboxEntity(productInboxMessage)
                )
        );
    }
    
    @Override
    public List<ProductInboxMessage> saveAll(List<ProductInboxMessage> productInboxMessages) {
        return productInboxJpaRepository.saveAll(
                productInboxMessages.stream()
                        .map(productInboxDataAccessMapper::productInboxMessageToProductInboxEntity)
                        .collect(Collectors.toList())
        ).stream()
                .map(productInboxDataAccessMapper::productInboxEntityToProductInboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductInboxMessage> findBySagaIdAndType(UUID sagaId, ServiceMessageType type) {
        return productInboxJpaRepository.findBySagaIdAndType(sagaId, type)
                .map(productInboxDataAccessMapper::productInboxEntityToProductInboxMessage);
    }
    
    @Override
    public List<ProductInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit) {
        return productInboxJpaRepository.findByStatusOrderByReceivedAt(status, PageRequest.of(0, limit))
                .stream()
                .map(productInboxDataAccessMapper::productInboxEntityToProductInboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        return productInboxJpaRepository.findByStatusAndRetryCountLessThanOrderByReceivedAt(
                        status, maxRetryCount, PageRequest.of(0, limit))
                .stream()
                .map(productInboxDataAccessMapper::productInboxEntityToProductInboxMessage)
                .collect(Collectors.toList());
    }
} 