package com.commerce.platform.product.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.inbox.mapper.ProductInboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.inbox.repository.ProductInboxJpaRepository;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import static org.jooq.impl.DSL.*;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ProductInboxRepositoryImpl implements ProductInboxRepository {

    private final ProductInboxJpaRepository productInboxJpaRepository;
    private final ProductInboxDataAccessMapper productInboxDataAccessMapper;
    private final DSLContext dsl;

    public ProductInboxRepositoryImpl(ProductInboxJpaRepository productInboxJpaRepository,
                                     ProductInboxDataAccessMapper productInboxDataAccessMapper,
                                     DSLContext dsl) {
        this.productInboxJpaRepository = productInboxJpaRepository;
        this.productInboxDataAccessMapper = productInboxDataAccessMapper;
        this.dsl = dsl;
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
        
        int insertedCount = 0;
        for (ProductInboxMessage message : productInboxMessages) {
            int result = dsl.insertInto(table("product_inbox"))
                .columns(
                    field("id"),
                    field("message_id"),
                    field("saga_id"),
                    field("type"),
                    field("payload"),
                    field("status"),
                    field("received_at"),
                    field("retry_count")
                )
                .values(
                    message.getId(),
                    message.getMessageId(),
                    message.getSagaId(),
                    message.getType().name(),
                    message.getPayload(),
                    message.getStatus().name(),
                    Timestamp.from(message.getReceivedAt().toInstant()),
                    message.getRetryCount()
                )
                .onDuplicateKeyIgnore()
                .execute();
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
        var result = dsl.selectFrom(table("product_inbox"))
            .where(field("status").eq(status.name()))
            .orderBy(field("received_at"))
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToProductInboxMessage);
    }
    
    @Override
    public List<ProductInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        var result = dsl.selectFrom(table("product_inbox"))
            .where(field("status").eq(status.name())
                .and(field("retry_count").lt(maxRetryCount)))
            .orderBy(field("received_at"))
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToProductInboxMessage);
    }
    
    private ProductInboxMessage mapToProductInboxMessage(Record record) {
        return ProductInboxMessage.builder()
                .id(record.getValue("id", UUID.class))
                .messageId(record.getValue("message_id", UUID.class))
                .sagaId(record.getValue("saga_id", UUID.class))
                .type(ServiceMessageType.valueOf(record.getValue("type", String.class)))
                .payload(record.getValue("payload", String.class))
                .status(InboxStatus.valueOf(record.getValue("status", String.class)))
                .receivedAt(ZonedDateTime.ofInstant(
                    record.getValue("received_at", Timestamp.class).toInstant(), 
                    ZoneOffset.UTC))
                .processedAt(record.getValue("processed_at", Timestamp.class) != null ? 
                    ZonedDateTime.ofInstant(
                        record.getValue("processed_at", Timestamp.class).toInstant(), 
                        ZoneOffset.UTC) : null)
                .retryCount(record.getValue("retry_count", Integer.class))
                .errorMessage(record.getValue("error_message", String.class))
                .build();
    }
} 