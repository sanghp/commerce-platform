package com.commerce.platform.product.service.domain.ports.input.service;

import com.commerce.platform.product.service.domain.dto.create.CreateProductCommand;
import com.commerce.platform.product.service.domain.dto.create.CreateProductResponse;
import com.commerce.platform.product.service.domain.dto.query.SearchProductsQuery;
import com.commerce.platform.product.service.domain.dto.query.SearchProductsResponse;

import jakarta.validation.Valid;

public interface ProductApplicationService {

    CreateProductResponse createProduct(@Valid CreateProductCommand createProductCommand);

    SearchProductsResponse searchProducts(@Valid SearchProductsQuery searchProductsQuery);
} 