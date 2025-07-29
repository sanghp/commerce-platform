package com.commerce.platform.product.service.dataaccess.inbox.repository;

import com.commerce.platform.product.service.dataaccess.inbox.entity.ProductInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductInboxJpaRepository extends JpaRepository<ProductInboxEntity, UUID> {

    Optional<ProductInboxEntity> findBySagaIdAndEventType(UUID sagaId, String eventType);
} 