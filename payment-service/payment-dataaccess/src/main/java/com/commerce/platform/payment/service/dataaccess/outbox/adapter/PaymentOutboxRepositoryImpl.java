package com.commerce.platform.payment.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.dataaccess.outbox.mapper.PaymentOutboxDataAccessMapper;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.commerce.platform.payment.service.dataaccess.jooq.Tables.PAYMENT_OUTBOX;

@RequiredArgsConstructor
@Component
public class PaymentOutboxRepositoryImpl implements PaymentOutboxRepository {
    
    private final DSLContext dsl;
    private final PaymentOutboxDataAccessMapper paymentOutboxDataAccessMapper;
    
    @Override
    public PaymentOutboxMessage save(PaymentOutboxMessage paymentOutboxMessage) {
        dsl.insertInto(PAYMENT_OUTBOX)
                .set(PAYMENT_OUTBOX.ID, paymentOutboxMessage.getId())
                .set(PAYMENT_OUTBOX.MESSAGE_ID, paymentOutboxMessage.getMessageId())
                .set(PAYMENT_OUTBOX.SAGA_ID, paymentOutboxMessage.getSagaId())
                .set(PAYMENT_OUTBOX.CREATED_AT, paymentOutboxMessage.getCreatedAt().toLocalDateTime())
                .set(PAYMENT_OUTBOX.FETCHED_AT, paymentOutboxMessage.getFetchedAt() != null ? paymentOutboxMessage.getFetchedAt().toLocalDateTime() : null)
                .set(PAYMENT_OUTBOX.PROCESSED_AT, paymentOutboxMessage.getProcessedAt() != null ? paymentOutboxMessage.getProcessedAt().toLocalDateTime() : null)
                .set(PAYMENT_OUTBOX.TYPE, paymentOutboxMessage.getType().name())
                .set(PAYMENT_OUTBOX.PAYLOAD, JSON.json(paymentOutboxMessage.getPayload()))
                .set(PAYMENT_OUTBOX.OUTBOX_STATUS, paymentOutboxMessage.getOutboxStatus().name())
                .set(PAYMENT_OUTBOX.VERSION, paymentOutboxMessage.getVersion())
                .onDuplicateKeyUpdate()
                .set(PAYMENT_OUTBOX.FETCHED_AT, paymentOutboxMessage.getFetchedAt() != null ? paymentOutboxMessage.getFetchedAt().toLocalDateTime() : null)
                .set(PAYMENT_OUTBOX.PROCESSED_AT, paymentOutboxMessage.getProcessedAt() != null ? paymentOutboxMessage.getProcessedAt().toLocalDateTime() : null)
                .set(PAYMENT_OUTBOX.OUTBOX_STATUS, paymentOutboxMessage.getOutboxStatus().name())
                .set(PAYMENT_OUTBOX.VERSION, paymentOutboxMessage.getVersion())
                .execute();
        
        return paymentOutboxMessage;
    }
    
    @Override
    public List<PaymentOutboxMessage> saveAll(List<PaymentOutboxMessage> paymentOutboxMessages) {
        if (paymentOutboxMessages.isEmpty()) {
            return paymentOutboxMessages;
        }
        
        var insertQuery = dsl.insertInto(PAYMENT_OUTBOX,
                PAYMENT_OUTBOX.ID,
                PAYMENT_OUTBOX.MESSAGE_ID,
                PAYMENT_OUTBOX.SAGA_ID,
                PAYMENT_OUTBOX.CREATED_AT,
                PAYMENT_OUTBOX.FETCHED_AT,
                PAYMENT_OUTBOX.PROCESSED_AT,
                PAYMENT_OUTBOX.TYPE,
                PAYMENT_OUTBOX.PAYLOAD,
                PAYMENT_OUTBOX.OUTBOX_STATUS,
                PAYMENT_OUTBOX.VERSION);
        
        for (PaymentOutboxMessage message : paymentOutboxMessages) {
            insertQuery = insertQuery.values(
                    message.getId(),
                    message.getMessageId(),
                    message.getSagaId(),
                    message.getCreatedAt().toLocalDateTime(),
                    message.getFetchedAt() != null ? message.getFetchedAt().toLocalDateTime() : null,
                    message.getProcessedAt() != null ? message.getProcessedAt().toLocalDateTime() : null,
                    message.getType().name(),
                    JSON.json(message.getPayload()),
                    message.getOutboxStatus().name(),
                    message.getVersion()
            );
        }
        
        insertQuery.onDuplicateKeyIgnore().execute();
        return paymentOutboxMessages;
    }
    
    @Override
    public Optional<PaymentOutboxMessage> findById(UUID id) {
        var record = dsl.selectFrom(PAYMENT_OUTBOX)
                .where(PAYMENT_OUTBOX.ID.eq(id))
                .fetchOne();
        
        if (record == null) {
            return Optional.empty();
        }
        
        return Optional.of(mapToPaymentOutboxMessage(record));
    }
    
    @Override
    public List<PaymentOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        var result = dsl.selectFrom(PAYMENT_OUTBOX)
                .where(PAYMENT_OUTBOX.OUTBOX_STATUS.eq(outboxStatus.name()))
                .orderBy(PAYMENT_OUTBOX.CREATED_AT)
                .limit(limit)
                .fetch();
        
        return result.stream()
                .map(this::mapToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PaymentOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        var result = dsl.selectFrom(PAYMENT_OUTBOX)
                .where(PAYMENT_OUTBOX.OUTBOX_STATUS.eq(outboxStatus.name()))
                .orderBy(PAYMENT_OUTBOX.CREATED_AT)
                .limit(limit)
                .forUpdate().skipLocked()
                .fetch();
        
        return result.stream()
                .map(this::mapToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PaymentOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        var result = dsl.selectFrom(PAYMENT_OUTBOX)
                .where(PAYMENT_OUTBOX.OUTBOX_STATUS.eq(outboxStatus.name())
                        .and(PAYMENT_OUTBOX.FETCHED_AT.lt(fetchedAtBefore.toLocalDateTime())))
                .orderBy(PAYMENT_OUTBOX.CREATED_AT)
                .limit(limit)
                .fetch();
        
        return result.stream()
                .map(this::mapToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public int deleteByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        var idsToDelete = dsl.select(PAYMENT_OUTBOX.ID)
                .from(PAYMENT_OUTBOX)
                .where(PAYMENT_OUTBOX.OUTBOX_STATUS.eq(outboxStatus.name()))
                .orderBy(PAYMENT_OUTBOX.CREATED_AT)
                .limit(limit)
                .fetch(PAYMENT_OUTBOX.ID);
        
        if (idsToDelete.isEmpty()) {
            return 0;
        }
        
        return dsl.deleteFrom(PAYMENT_OUTBOX)
                .where(PAYMENT_OUTBOX.ID.in(idsToDelete))
                .execute();
    }
    
    @Override
    @Transactional
    public int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt) {
        if (ids.isEmpty()) {
            return 0;
        }
        
        LocalDateTime fetchedAtLocal = fetchedAt != null ? fetchedAt.toLocalDateTime() : null;
        
        return dsl.update(PAYMENT_OUTBOX)
                .set(PAYMENT_OUTBOX.OUTBOX_STATUS, status.name())
                .set(PAYMENT_OUTBOX.FETCHED_AT, fetchedAtLocal)
                .where(PAYMENT_OUTBOX.ID.in(ids))
                .execute();
    }
    
    private PaymentOutboxMessage mapToPaymentOutboxMessage(org.jooq.Record record) {
        return PaymentOutboxMessage.builder()
                .id(record.get(PAYMENT_OUTBOX.ID))
                .messageId(record.get(PAYMENT_OUTBOX.MESSAGE_ID))
                .sagaId(record.get(PAYMENT_OUTBOX.SAGA_ID))
                .createdAt(record.get(PAYMENT_OUTBOX.CREATED_AT).atZone(ZoneOffset.UTC))
                .fetchedAt(record.get(PAYMENT_OUTBOX.FETCHED_AT) != null ? 
                        record.get(PAYMENT_OUTBOX.FETCHED_AT).atZone(ZoneOffset.UTC) : null)
                .processedAt(record.get(PAYMENT_OUTBOX.PROCESSED_AT) != null ? 
                        record.get(PAYMENT_OUTBOX.PROCESSED_AT).atZone(ZoneOffset.UTC) : null)
                .type(ServiceMessageType.valueOf(record.get(PAYMENT_OUTBOX.TYPE)))
                .payload(record.get(PAYMENT_OUTBOX.PAYLOAD).data())
                .outboxStatus(OutboxStatus.valueOf(record.get(PAYMENT_OUTBOX.OUTBOX_STATUS)))
                .version(record.get(PAYMENT_OUTBOX.VERSION))
                .build();
    }
}