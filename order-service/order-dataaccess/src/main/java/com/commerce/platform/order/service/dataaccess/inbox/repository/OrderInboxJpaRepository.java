package com.commerce.platform.order.service.dataaccess.inbox.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.dataaccess.inbox.entity.OrderInboxEntity;
import com.commerce.platform.inbox.InboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderInboxJpaRepository extends JpaRepository<OrderInboxEntity, UUID> {
    Optional<OrderInboxEntity> findByMessageId(UUID messageId);
    
    @Query(value = "SELECT * FROM order_inbox WHERE status = :#{#status.name()} ORDER BY received_at LIMIT :#{#pageable.pageSize} FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<OrderInboxEntity> findByStatusOrderByReceivedAt(InboxStatus status, Pageable pageable);
    
    @Query(value = "SELECT * FROM order_inbox WHERE status = :#{#status.name()} AND retry_count < :maxRetryCount ORDER BY received_at LIMIT :#{#pageable.pageSize} FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<OrderInboxEntity> findByStatusAndRetryCountLessThanOrderByReceivedAt(InboxStatus status, Integer maxRetryCount, Pageable pageable);
}