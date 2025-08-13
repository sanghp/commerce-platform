package com.commerce.platform.payment.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.dataaccess.inbox.mapper.PaymentInboxDataAccessMapper;
import com.commerce.platform.payment.service.dataaccess.inbox.repository.PaymentInboxJpaRepository;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentInboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Component;

import static org.jooq.impl.DSL.*;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class PaymentInboxRepositoryImpl implements PaymentInboxRepository {
    
    private final PaymentInboxJpaRepository paymentInboxJpaRepository;
    private final PaymentInboxDataAccessMapper paymentInboxDataAccessMapper;
    private final DSLContext dsl;

    public PaymentInboxRepositoryImpl(PaymentInboxJpaRepository paymentInboxJpaRepository,
                                     PaymentInboxDataAccessMapper paymentInboxDataAccessMapper,
                                     DSLContext dsl) {
        this.paymentInboxJpaRepository = paymentInboxJpaRepository;
        this.paymentInboxDataAccessMapper = paymentInboxDataAccessMapper;
        this.dsl = dsl;
    }
    
    @Override
    public PaymentInboxMessage save(PaymentInboxMessage paymentInboxMessage) {
        return paymentInboxDataAccessMapper.inboxEntityToPaymentInboxMessage(
                paymentInboxJpaRepository.save(
                        paymentInboxDataAccessMapper.paymentInboxMessageToInboxEntity(paymentInboxMessage)));
    }
    
    @Override
    public Optional<PaymentInboxMessage> findByMessageId(UUID messageId) {
        return paymentInboxJpaRepository.findByMessageId(messageId)
                .map(paymentInboxDataAccessMapper::inboxEntityToPaymentInboxMessage);
    }
    
    @Override
    public List<PaymentInboxMessage> saveAll(List<PaymentInboxMessage> paymentInboxMessages) {
        if (paymentInboxMessages.isEmpty()) {
            return paymentInboxMessages;
        }
        
        int insertedCount = 0;
        for (PaymentInboxMessage message : paymentInboxMessages) {
            int result = dsl.insertInto(table("payment_inbox"))
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
        
        log.info("Inserted {} new messages out of {} total messages to inbox", insertedCount, paymentInboxMessages.size());
        
        return paymentInboxMessages;
    }
    
    @Override
    public List<PaymentInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit) {
        var result = dsl.selectFrom(table("payment_inbox"))
            .where(field("status").eq(status.name()))
            .orderBy(field("received_at"))
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToPaymentInboxMessage);
    }
    
    @Override
    public List<PaymentInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        var result = dsl.selectFrom(table("payment_inbox"))
            .where(field("status").eq(status.name())
                .and(field("retry_count").lt(maxRetryCount)))
            .orderBy(field("received_at"))
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToPaymentInboxMessage);
    }
    
    private PaymentInboxMessage mapToPaymentInboxMessage(Record record) {
        return PaymentInboxMessage.builder()
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