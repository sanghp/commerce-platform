package com.commerce.platform.order.service.dataaccess.outbox.repository;

import com.commerce.platform.order.service.dataaccess.outbox.entity.OrderOutboxEntity;
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
public interface OrderOutboxJpaRepository extends JpaRepository<OrderOutboxEntity, UUID> {

    List<OrderOutboxEntity> findByOutboxStatusOrderByCreatedAt(OutboxStatus outboxStatus, Pageable pageable);
    
    @Query(value = "SELECT * FROM order_outbox WHERE outbox_status = :status ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<OrderOutboxEntity> findByOutboxStatusWithSkipLock(@Param("status") String status, @Param("limit") int limit);
    
    List<OrderOutboxEntity> findByOutboxStatusAndFetchedAtBeforeOrderByCreatedAt(OutboxStatus outboxStatus, 
                                                                                 ZonedDateTime fetchedAtBefore, 
                                                                                 Pageable pageable);

}