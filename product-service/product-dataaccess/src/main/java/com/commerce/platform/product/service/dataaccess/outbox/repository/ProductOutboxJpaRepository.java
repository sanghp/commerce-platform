package com.commerce.platform.product.service.dataaccess.outbox.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.dataaccess.outbox.entity.ProductOutboxEntity;
import com.commerce.platform.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductOutboxJpaRepository extends JpaRepository<ProductOutboxEntity, UUID> {

    void deleteByOutboxStatus(OutboxStatus outboxStatus);
    
    List<ProductOutboxEntity> findByOutboxStatus(OutboxStatus outboxStatus, Pageable pageable);
    
    List<ProductOutboxEntity> findByOutboxStatusAndFetchedAtBeforeOrderByCreatedAt(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore);
    
    @Query(value = "SELECT * FROM product_outbox WHERE outbox_status = :outboxStatus ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<ProductOutboxEntity> findByOutboxStatusWithSkipLock(@Param("outboxStatus") String outboxStatus, @Param("limit") int limit);
}