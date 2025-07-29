package com.commerce.platform.product.service.dataaccess.product.repository;

import com.commerce.platform.product.service.dataaccess.product.entity.ProductReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductReservationJpaRepository extends JpaRepository<ProductReservationEntity, UUID> {
    
    List<ProductReservationEntity> findByOrderId(UUID orderId);
    
    List<ProductReservationEntity> findByProductId(UUID productId);
}