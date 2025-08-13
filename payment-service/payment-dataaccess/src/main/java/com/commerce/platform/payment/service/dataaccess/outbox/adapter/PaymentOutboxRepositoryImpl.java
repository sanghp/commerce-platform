package com.commerce.platform.payment.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.dataaccess.outbox.mapper.PaymentOutboxDataAccessMapper;
import com.commerce.platform.payment.service.dataaccess.outbox.repository.PaymentOutboxJpaRepository;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentOutboxRepository;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.JSON;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.jooq.impl.DSL.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PaymentOutboxRepositoryImpl implements PaymentOutboxRepository {

    private final PaymentOutboxJpaRepository paymentOutboxJpaRepository;
    private final PaymentOutboxDataAccessMapper paymentOutboxDataAccessMapper;
    private final DSLContext dsl;

    public PaymentOutboxRepositoryImpl(PaymentOutboxJpaRepository paymentOutboxJpaRepository,
                                      PaymentOutboxDataAccessMapper paymentOutboxDataAccessMapper,
                                      DSLContext dsl) {
        this.paymentOutboxJpaRepository = paymentOutboxJpaRepository;
        this.paymentOutboxDataAccessMapper = paymentOutboxDataAccessMapper;
        this.dsl = dsl;
    }

    @Override
    public PaymentOutboxMessage save(PaymentOutboxMessage paymentOutboxMessage) {
        return paymentOutboxDataAccessMapper.outboxEntityToPaymentOutboxMessage(
                paymentOutboxJpaRepository.save(
                        paymentOutboxDataAccessMapper.paymentOutboxMessageToOutboxEntity(paymentOutboxMessage)));
    }

    @Override
    public List<PaymentOutboxMessage> saveAll(List<PaymentOutboxMessage> paymentOutboxMessages) {
        return paymentOutboxJpaRepository.saveAll(
                paymentOutboxMessages.stream()
                        .map(paymentOutboxDataAccessMapper::paymentOutboxMessageToOutboxEntity)
                        .collect(Collectors.toList())
        ).stream()
                .map(paymentOutboxDataAccessMapper::outboxEntityToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PaymentOutboxMessage> findById(UUID id) {
        return paymentOutboxJpaRepository.findById(id)
                .map(paymentOutboxDataAccessMapper::outboxEntityToPaymentOutboxMessage);
    }

    @Override
    public List<PaymentOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return paymentOutboxJpaRepository.findByOutboxStatusOrderByCreatedAt(outboxStatus)
                .stream()
                .limit(limit)
                .map(paymentOutboxDataAccessMapper::outboxEntityToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        var result = dsl.selectFrom(table("payment_outbox"))
            .where(field("outbox_status").eq(outboxStatus.name()))
            .orderBy(field("created_at"))
            .limit(limit)
            .forUpdate().skipLocked()
            .fetch();
        
        return result.map(this::mapToPaymentOutboxMessage);
    }

    @Override
    public List<PaymentOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        var result = dsl.selectFrom(table("payment_outbox"))
            .where(field("outbox_status").eq(outboxStatus.name())
                .and(field("fetched_at").lt(java.sql.Timestamp.from(fetchedAtBefore.toInstant()))))
            .orderBy(field("created_at"))
            .limit(limit)
            .fetch();
        
        return result.map(this::mapToPaymentOutboxMessage);
    }

    @Override
    public int deleteByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        var entitiesToDelete = paymentOutboxJpaRepository
                .findByOutboxStatusOrderByCreatedAt(outboxStatus)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        paymentOutboxJpaRepository.deleteAll(entitiesToDelete);
        return entitiesToDelete.size();
    }

    @Override
    @Transactional
    public int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt) {
        if (ids.isEmpty()) {
            return 0;
        }

        return dsl.update(table("payment_outbox"))
            .set(field("outbox_status"), status.name())
            .set(field("fetched_at"), fetchedAt != null ? 
                java.sql.Timestamp.from(fetchedAt.toInstant()) : null)
            .where(field("id").in(ids))
            .execute();
    }
    
    private PaymentOutboxMessage mapToPaymentOutboxMessage(Record record) {
        return PaymentOutboxMessage.builder()
                .id(record.getValue("id", UUID.class))
                .messageId(record.getValue("message_id", UUID.class))
                .sagaId(record.getValue("saga_id", UUID.class))
                .type(ServiceMessageType.valueOf(record.getValue("type", String.class)))
                .payload(record.getValue("payload", String.class))
                .outboxStatus(OutboxStatus.valueOf(record.getValue("outbox_status", String.class)))
                .createdAt(ZonedDateTime.ofInstant(
                    record.getValue("created_at", java.sql.Timestamp.class).toInstant(), 
                    ZoneOffset.UTC))
                .processedAt(record.getValue("processed_at", java.sql.Timestamp.class) != null ? 
                    ZonedDateTime.ofInstant(
                        record.getValue("processed_at", java.sql.Timestamp.class).toInstant(), 
                        ZoneOffset.UTC) : null)
                .fetchedAt(record.getValue("fetched_at", java.sql.Timestamp.class) != null ? 
                    ZonedDateTime.ofInstant(
                        record.getValue("fetched_at", java.sql.Timestamp.class).toInstant(), 
                        ZoneOffset.UTC) : null)
                .version(record.getValue("version", Integer.class))
                .build();
    }
}