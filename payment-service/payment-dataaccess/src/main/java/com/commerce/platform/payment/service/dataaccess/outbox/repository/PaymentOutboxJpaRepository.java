package com.commerce.platform.payment.service.dataaccess.outbox.repository;

import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.dataaccess.outbox.entity.PaymentOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutboxEntity, UUID> {
    
    @Query(value = "SELECT * FROM payment_outbox WHERE outbox_status = :status ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<PaymentOutboxEntity> findByOutboxStatusWithSkipLock(@Param("status") String status, @Param("limit") int limit);
    
    List<PaymentOutboxEntity> findByOutboxStatusOrderByCreatedAt(OutboxStatus outboxStatus);
    
    List<PaymentOutboxEntity> findByOutboxStatusAndFetchedAtBeforeOrderByCreatedAt(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore);
}