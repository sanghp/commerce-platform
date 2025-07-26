package com.commerce.platform.product.service.domain.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SearchProductsResponse {
    private final List<ProductResponse> products;
} 