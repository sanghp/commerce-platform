package com.commerce.platform.product.service.dataaccess.product.repository;

import com.commerce.platform.product.service.dataaccess.product.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID>, ProductCustomRepository {

    List<ProductEntity> findByIdIn(List<UUID> productIds);

} 