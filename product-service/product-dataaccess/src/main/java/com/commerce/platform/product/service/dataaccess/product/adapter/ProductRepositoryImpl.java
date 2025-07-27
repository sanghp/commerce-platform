package com.commerce.platform.product.service.dataaccess.product.adapter;

import com.commerce.platform.product.service.dataaccess.product.entity.ProductEntity;
import com.commerce.platform.product.service.dataaccess.product.mapper.ProductDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.product.repository.ProductJpaRepository;
import com.commerce.platform.product.service.domain.entity.Product;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ProductRepositoryImpl implements ProductRepository {
    
    private final ProductJpaRepository productJpaRepository;
    private final ProductDataAccessMapper productDataAccessMapper;

    public ProductRepositoryImpl(ProductJpaRepository productJpaRepository,
                                 ProductDataAccessMapper productDataAccessMapper) {
        this.productJpaRepository = productJpaRepository;
        this.productDataAccessMapper = productDataAccessMapper;
    }

    @Override
    public Product save(Product product) {
        ProductEntity productEntity = productDataAccessMapper.productToProductEntity(product);
        ProductEntity savedEntity = productJpaRepository.save(productEntity);
        log.info("Product is saved with id: {}", savedEntity.getId());
        return productDataAccessMapper.productEntityToProduct(savedEntity);
    }

    @Override
    public List<Product> saveAll(List<Product> products) {
        List<ProductEntity> productEntities = products.stream()
                .map(productDataAccessMapper::productToProductEntity)
                .toList();
        List<ProductEntity> savedEntities = productJpaRepository.saveAll(productEntities);
        log.info("{} products are saved", savedEntities.size());
        return productDataAccessMapper.productEntitiesToProducts(savedEntities);
    }

    @Override
    public Optional<Product> findById(UUID productId) {
        return productJpaRepository.findById(productId)
                .map(productDataAccessMapper::productEntityToProduct);
    }

    @Override
    public List<Product> findByIds(List<UUID> productIds) {
        List<ProductEntity> productEntities = productJpaRepository.findByIdIn(productIds);
        log.info("Found {} products for {} requested product IDs", productEntities.size(), productIds.size());
        return productDataAccessMapper.productEntitiesToProducts(productEntities);
    }

    @Override
    public List<Product> findByIdsForUpdate(List<UUID> productIds) {
        List<ProductEntity> productEntities = productJpaRepository.findByIdsForUpdate(productIds);
        log.info("Found {} products with X-lock for {} requested product IDs", productEntities.size(), productIds.size());
        return productDataAccessMapper.productEntitiesToProducts(productEntities);
    }
} 