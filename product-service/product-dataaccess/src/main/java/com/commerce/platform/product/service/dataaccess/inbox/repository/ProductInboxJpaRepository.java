package com.commerce.platform.product.service.dataaccess.inbox.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.inbox.entity.ProductInboxEntity;
import com.commerce.platform.inbox.InboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductInboxJpaRepository extends JpaRepository<ProductInboxEntity, UUID> {
    
    Optional<ProductInboxEntity> findByMessageId(UUID messageId);
    
    @Query(value = "SELECT * FROM product_inbox WHERE status = :#{#status.name()} ORDER BY received_at LIMIT :#{#pageable.pageSize} FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<ProductInboxEntity> findByStatusOrderByReceivedAt(InboxStatus status, Pageable pageable);
    
    @Query(value = "SELECT * FROM product_inbox WHERE status = :#{#status.name()} AND retry_count < :maxRetryCount ORDER BY received_at LIMIT :#{#pageable.pageSize} FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<ProductInboxEntity> findByStatusAndRetryCountLessThanOrderByReceivedAt(InboxStatus status, int maxRetryCount, Pageable pageable);
} 