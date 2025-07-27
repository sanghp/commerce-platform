package com.commerce.platform.product.service.dataaccess.product.mapper;

import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.product.service.dataaccess.product.entity.ProductEntity;
import com.commerce.platform.product.service.domain.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductDataAccessMapper {

    public ProductEntity productToProductEntity(Product product) {
        return ProductEntity.builder()
                .id(product.getId().getValue())
                .name(product.getName())
                .price(product.getPrice().getAmount())
                .quantity(product.getQuantity())
                .reservedQuantity(product.getReservedQuantity())
                .enabled(product.isEnabled())
                .createdAt(product.getCreatedAt())
                .build();
    }

    public Product productEntityToProduct(ProductEntity productEntity) {
        return Product.builder()
                .productId(new ProductId(productEntity.getId()))
                .name(productEntity.getName())
                .price(new Money(productEntity.getPrice()))
                .quantity(productEntity.getQuantity())
                .reservedQuantity(productEntity.getReservedQuantity())
                .enabled(productEntity.getEnabled())
                .createdAt(productEntity.getCreatedAt())
                .build();
    }

    public List<Product> productEntitiesToProducts(List<ProductEntity> productEntities) {
        return productEntities.stream()
                .map(this::productEntityToProduct)
                .collect(Collectors.toList());
    }
} 