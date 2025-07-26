package com.commerce.platform.product.service.domain.ports.output.repository;

import com.commerce.platform.product.service.domain.entity.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    List<Product> saveAll(List<Product> products);

    Optional<Product> findById(UUID productId);

    List<Product> findByIds(List<UUID> productIds);

    List<Product> findByIdsForUpdate(List<UUID> productIds);

} 