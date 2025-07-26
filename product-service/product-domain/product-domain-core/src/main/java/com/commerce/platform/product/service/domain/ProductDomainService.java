package com.commerce.platform.product.service.domain;

import com.commerce.platform.product.service.domain.entity.Product;

import java.time.ZonedDateTime;

public interface ProductDomainService {

    Product createProduct(Product product, ZonedDateTime requestTime);

    Product reserveProduct(Product product, Integer requestedQuantity, ZonedDateTime requestTime);

}
