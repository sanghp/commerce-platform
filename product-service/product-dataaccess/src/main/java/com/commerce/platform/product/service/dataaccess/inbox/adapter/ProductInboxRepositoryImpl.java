package com.commerce.platform.product.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.inbox.mapper.ProductInboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.inbox.repository.ProductInboxJpaRepository;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductInboxRepositoryImpl implements ProductInboxRepository {

    private final ProductInboxJpaRepository productInboxJpaRepository;
    private final ProductInboxDataAccessMapper productInboxDataAccessMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        if (productInboxMessages.isEmpty()) {
            return productInboxMessages;
        }
        
        String sql = "INSERT IGNORE INTO product_inbox (id, message_id, saga_id, type, payload, status, received_at, retry_count) " +
                     "VALUES (UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')), ?, ?, ?, ?, ?)";
        
        int insertedCount = 0;
        for (ProductInboxMessage message : productInboxMessages) {
            int result = jdbcTemplate.update(sql,
                message.getId().toString(),
                message.getMessageId().toString(),
                message.getSagaId().toString(),
                message.getType().name(),
                message.getPayload(),
                message.getStatus().name(),
                Timestamp.from(message.getReceivedAt().toInstant()),
                message.getRetryCount()
            );
            insertedCount += result;
        }
        
        log.info("Inserted {} new messages out of {} total messages to inbox", insertedCount, productInboxMessages.size());
        
        return productInboxMessages;
    }

    @Override
    public Optional<ProductInboxMessage> findByMessageId(UUID messageId) {
        return productInboxJpaRepository.findByMessageId(messageId)
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