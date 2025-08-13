package com.commerce.platform.payment.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.dataaccess.inbox.mapper.PaymentInboxDataAccessMapper;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.commerce.platform.payment.service.dataaccess.jooq.Tables.PAYMENT_INBOX;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentInboxRepositoryImpl implements PaymentInboxRepository {
    
    private final DSLContext dsl;
    private final PaymentInboxDataAccessMapper paymentInboxDataAccessMapper;
    
    @Override
    public PaymentInboxMessage save(PaymentInboxMessage paymentInboxMessage) {
        dsl.insertInto(PAYMENT_INBOX)
                .set(PAYMENT_INBOX.ID, paymentInboxMessage.getId())
                .set(PAYMENT_INBOX.MESSAGE_ID, paymentInboxMessage.getMessageId())
                .set(PAYMENT_INBOX.SAGA_ID, paymentInboxMessage.getSagaId())
                .set(PAYMENT_INBOX.TYPE, paymentInboxMessage.getType().name())
                .set(PAYMENT_INBOX.PAYLOAD, JSON.json(paymentInboxMessage.getPayload()))
                .set(PAYMENT_INBOX.STATUS, paymentInboxMessage.getStatus().name())
                .set(PAYMENT_INBOX.RECEIVED_AT, paymentInboxMessage.getReceivedAt().toLocalDateTime())
                .set(PAYMENT_INBOX.PROCESSED_AT, paymentInboxMessage.getProcessedAt() != null ? paymentInboxMessage.getProcessedAt().toLocalDateTime() : null)
                .set(PAYMENT_INBOX.RETRY_COUNT, paymentInboxMessage.getRetryCount())
                .set(PAYMENT_INBOX.ERROR_MESSAGE, paymentInboxMessage.getErrorMessage())
                .onDuplicateKeyUpdate()
                .set(PAYMENT_INBOX.STATUS, paymentInboxMessage.getStatus().name())
                .set(PAYMENT_INBOX.PROCESSED_AT, paymentInboxMessage.getProcessedAt() != null ? paymentInboxMessage.getProcessedAt().toLocalDateTime() : null)
                .set(PAYMENT_INBOX.RETRY_COUNT, paymentInboxMessage.getRetryCount())
                .set(PAYMENT_INBOX.ERROR_MESSAGE, paymentInboxMessage.getErrorMessage())
                .execute();
        
        return paymentInboxMessage;
    }
    
    @Override
    public Optional<PaymentInboxMessage> findByMessageId(UUID messageId) {
        var record = dsl.selectFrom(PAYMENT_INBOX)
                .where(PAYMENT_INBOX.MESSAGE_ID.eq(messageId))
                .fetchOne();
        
        if (record == null) {
            return Optional.empty();
        }
        
        return Optional.of(mapToPaymentInboxMessage(record));
    }
    
    @Override
    public List<PaymentInboxMessage> saveAll(List<PaymentInboxMessage> paymentInboxMessages) {
        if (paymentInboxMessages.isEmpty()) {
            return paymentInboxMessages;
        }
        
        var insertQuery = dsl.insertInto(PAYMENT_INBOX,
                PAYMENT_INBOX.ID,
                PAYMENT_INBOX.MESSAGE_ID,
                PAYMENT_INBOX.SAGA_ID,
                PAYMENT_INBOX.TYPE,
                PAYMENT_INBOX.PAYLOAD,
                PAYMENT_INBOX.STATUS,
                PAYMENT_INBOX.RECEIVED_AT,
                PAYMENT_INBOX.RETRY_COUNT);
        
        for (PaymentInboxMessage message : paymentInboxMessages) {
            insertQuery = insertQuery.values(
                    message.getId(),
                    message.getMessageId(),
                    message.getSagaId(),
                    message.getType().name(),
                    JSON.json(message.getPayload()),
                    message.getStatus().name(),
                    message.getReceivedAt().toLocalDateTime(),
                    message.getRetryCount()
            );
        }
        
        int insertedCount = insertQuery.onDuplicateKeyIgnore().execute();
        log.info("Inserted {} new messages out of {} total messages to inbox", insertedCount, paymentInboxMessages.size());
        
        return paymentInboxMessages;
    }
    
    @Override
    public List<PaymentInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit) {
        var result = dsl.selectFrom(PAYMENT_INBOX)
                .where(PAYMENT_INBOX.STATUS.eq(status.name()))
                .orderBy(PAYMENT_INBOX.RECEIVED_AT)
                .limit(limit)
                .forUpdate().skipLocked()
                .fetch();
        
        return result.stream()
                .map(this::mapToPaymentInboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PaymentInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        var result = dsl.selectFrom(PAYMENT_INBOX)
                .where(PAYMENT_INBOX.STATUS.eq(status.name())
                        .and(PAYMENT_INBOX.RETRY_COUNT.lt(maxRetryCount)))
                .orderBy(PAYMENT_INBOX.RECEIVED_AT)
                .limit(limit)
                .forUpdate().skipLocked()
                .fetch();
        
        return result.stream()
                .map(this::mapToPaymentInboxMessage)
                .collect(Collectors.toList());
    }
    
    private PaymentInboxMessage mapToPaymentInboxMessage(org.jooq.Record record) {
        return PaymentInboxMessage.builder()
                .id(record.get(PAYMENT_INBOX.ID))
                .messageId(record.get(PAYMENT_INBOX.MESSAGE_ID))
                .sagaId(record.get(PAYMENT_INBOX.SAGA_ID))
                .type(ServiceMessageType.valueOf(record.get(PAYMENT_INBOX.TYPE)))
                .payload(record.get(PAYMENT_INBOX.PAYLOAD).data())
                .status(InboxStatus.valueOf(record.get(PAYMENT_INBOX.STATUS)))
                .receivedAt(record.get(PAYMENT_INBOX.RECEIVED_AT).atZone(ZoneOffset.UTC))
                .processedAt(record.get(PAYMENT_INBOX.PROCESSED_AT) != null ? 
                        record.get(PAYMENT_INBOX.PROCESSED_AT).atZone(ZoneOffset.UTC) : null)
                .retryCount(record.get(PAYMENT_INBOX.RETRY_COUNT))
                .errorMessage(record.get(PAYMENT_INBOX.ERROR_MESSAGE))
                .build();
    }
}