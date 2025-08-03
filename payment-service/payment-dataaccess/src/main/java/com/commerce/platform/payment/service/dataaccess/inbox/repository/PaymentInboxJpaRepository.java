package com.commerce.platform.payment.service.dataaccess.inbox.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.dataaccess.inbox.entity.PaymentInboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentInboxJpaRepository extends JpaRepository<PaymentInboxEntity, UUID> {
    
    Optional<PaymentInboxEntity> findByMessageId(UUID messageId);
    
    @Query(value = "SELECT * FROM payment_inbox WHERE status = :status ORDER BY received_at LIMIT :#{#pageable.pageSize} FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<PaymentInboxEntity> findByStatusOrderByReceivedAtWithSkipLock(String status, Pageable pageable);
    
    @Query(value = "SELECT * FROM payment_inbox WHERE status = :status AND retry_count < :maxRetryCount ORDER BY received_at LIMIT :#{#pageable.pageSize} FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<PaymentInboxEntity> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(String status, Integer maxRetryCount, Pageable pageable);
}