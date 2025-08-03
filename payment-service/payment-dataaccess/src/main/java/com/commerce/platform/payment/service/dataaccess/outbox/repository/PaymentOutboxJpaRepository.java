package com.commerce.platform.payment.service.dataaccess.outbox.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.dataaccess.outbox.entity.PaymentOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutboxEntity, UUID> {
    
    
    List<PaymentOutboxEntity> findByTypeAndOutboxStatusOrderByCreatedAt(ServiceMessageType type, OutboxStatus outboxStatus);
    
    List<PaymentOutboxEntity> findByOutboxStatusOrderByCreatedAt(OutboxStatus outboxStatus);
    
    List<PaymentOutboxEntity> findByOutboxStatusAndFetchedAtBeforeOrderByCreatedAt(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore);
}