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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        String sql = "SELECT BIN_TO_UUID(id) as id, BIN_TO_UUID(message_id) as message_id, BIN_TO_UUID(saga_id) as saga_id, " +
                    "type, payload, status, received_at, processed_at, retry_count, error_message " +
                    "FROM product_inbox WHERE status = ? ORDER BY received_at LIMIT ? FOR UPDATE SKIP LOCKED";
        
        return jdbcTemplate.query(sql, new Object[]{status.name(), limit}, new ProductInboxRowMapper());
    }
    
    @Override
    public List<ProductInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        String sql = "SELECT BIN_TO_UUID(id) as id, BIN_TO_UUID(message_id) as message_id, BIN_TO_UUID(saga_id) as saga_id, " +
                    "type, payload, status, received_at, processed_at, retry_count, error_message " +
                    "FROM product_inbox WHERE status = ? AND retry_count < ? ORDER BY received_at LIMIT ? FOR UPDATE SKIP LOCKED";
        
        return jdbcTemplate.query(sql, new Object[]{status.name(), maxRetryCount, limit}, new ProductInboxRowMapper());
    }
    
    private static class ProductInboxRowMapper implements RowMapper<ProductInboxMessage> {
        @Override
        public ProductInboxMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ProductInboxMessage.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .messageId(UUID.fromString(rs.getString("message_id")))
                    .sagaId(UUID.fromString(rs.getString("saga_id")))
                    .type(ServiceMessageType.valueOf(rs.getString("type")))
                    .payload(rs.getString("payload"))
                    .status(InboxStatus.valueOf(rs.getString("status")))
                    .receivedAt(ZonedDateTime.ofInstant(rs.getTimestamp("received_at").toInstant(), ZoneId.of("UTC")))
                    .processedAt(rs.getTimestamp("processed_at") != null ? 
                            ZonedDateTime.ofInstant(rs.getTimestamp("processed_at").toInstant(), ZoneId.of("UTC")) : null)
                    .retryCount(rs.getInt("retry_count"))
                    .errorMessage(rs.getString("error_message"))
                    .build();
        }
    }
} 