package com.commerce.platform.payment.service.domain.ports.output.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOutboxRepository {
    
    PaymentOutboxMessage save(PaymentOutboxMessage paymentOutboxMessage);
    
    List<PaymentOutboxMessage> saveAll(List<PaymentOutboxMessage> paymentOutboxMessages);
    
    Optional<PaymentOutboxMessage> findById(UUID id);
    
    List<PaymentOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit);
    
    List<PaymentOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit);
    
    List<PaymentOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit);
    
    int deleteByOutboxStatus(OutboxStatus outboxStatus, int limit);
    
    int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt);
}