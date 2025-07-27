package com.commerce.platform.product.service.dataaccess.product.repository;

import com.commerce.platform.product.service.dataaccess.product.entity.ProductEntity;
import java.util.List;
import java.util.UUID;

public interface ProductCustomRepository {
    
    List<ProductEntity> findByIdsForUpdate(List<UUID> productIds);
} 