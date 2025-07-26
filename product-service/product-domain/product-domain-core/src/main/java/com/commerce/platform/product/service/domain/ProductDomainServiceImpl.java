package com.commerce.platform.product.service.domain;

import com.commerce.platform.product.service.domain.entity.Product;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;

@Slf4j
public class ProductDomainServiceImpl implements ProductDomainService {

    @Override
    public Product createProduct(Product product, ZonedDateTime requestTime) {
        product.setCreatedAt(requestTime);
        product.setReservedQuantity(0);
        product.validateProduct();
        log.info("Product is created with id: {} at {}", product.getId().getValue(), requestTime);
        return product;
    }

    @Override
    public Product reserveProduct(Product product, Integer requestedQuantity, ZonedDateTime requestTime) {
        product.validateAndReserveProduct(requestedQuantity);
        log.info("Product with id: {} is reserved with quantity: {} at {}", 
                product.getId().getValue(), requestedQuantity, requestTime);
        return product;
    }
} 