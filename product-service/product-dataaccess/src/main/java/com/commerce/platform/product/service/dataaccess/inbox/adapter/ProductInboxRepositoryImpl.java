package com.commerce.platform.product.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.inbox.mapper.ProductInboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.inbox.repository.ProductInboxJpaRepository;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import static com.commerce.platform.product.service.dataaccess.jooq.tables.ProductInbox.PRODUCT_INBOX;
import java.time.ZoneOffset;
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
            int result = dsl.insertInto(PRODUCT_INBOX)
                .columns(
                    PRODUCT_INBOX.ID,
                    PRODUCT_INBOX.MESSAGE_ID,
                    PRODUCT_INBOX.SAGA_ID,
                    PRODUCT_INBOX.TYPE,
                    PRODUCT_INBOX.PAYLOAD,
                    PRODUCT_INBOX.STATUS,
                    PRODUCT_INBOX.RECEIVED_AT,
                    PRODUCT_INBOX.RETRY_COUNT
                )
                .values(
                    message.getId(),
                    message.getMessageId(),
                    message.getSagaId(),
                    message.getType().name(),
                    JSON.json(message.getPayload()),
                    message.getStatus().name(),
                    message.getReceivedAt().toLocalDateTime(),
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
        var result = dsl.selectFrom(PRODUCT_INBOX)
            .where(PRODUCT_INBOX.STATUS.eq(status.name()))
            .orderBy(PRODUCT_INBOX.RECEIVED_AT)
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToProductInboxMessage);
    }
    
    @Override
    public List<ProductInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        var result = dsl.selectFrom(PRODUCT_INBOX)
            .where(PRODUCT_INBOX.STATUS.eq(status.name())
                .and(PRODUCT_INBOX.RETRY_COUNT.lt(maxRetryCount)))
            .orderBy(PRODUCT_INBOX.RECEIVED_AT)
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToProductInboxMessage);
    }
    
    private ProductInboxMessage mapToProductInboxMessage(Record record) {
        return ProductInboxMessage.builder()
                .id(record.getValue(PRODUCT_INBOX.ID))
                .messageId(record.getValue(PRODUCT_INBOX.MESSAGE_ID))
                .sagaId(record.getValue(PRODUCT_INBOX.SAGA_ID))
                .type(ServiceMessageType.valueOf(record.getValue(PRODUCT_INBOX.TYPE)))
                .payload(record.getValue(PRODUCT_INBOX.PAYLOAD).data())
                .status(InboxStatus.valueOf(record.getValue(PRODUCT_INBOX.STATUS)))
                .receivedAt(record.getValue(PRODUCT_INBOX.RECEIVED_AT).atZone(ZoneOffset.UTC))
                .processedAt(record.getValue(PRODUCT_INBOX.PROCESSED_AT) != null ? 
                    record.getValue(PRODUCT_INBOX.PROCESSED_AT).atZone(ZoneOffset.UTC) : null)
                .retryCount(record.getValue(PRODUCT_INBOX.RETRY_COUNT))
                .errorMessage(record.getValue(PRODUCT_INBOX.ERROR_MESSAGE))
                .build();
    }
}