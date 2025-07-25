package com.commerce.platform.order.service.domain.ports.output.client;

import com.commerce.platform.order.service.domain.dto.client.SearchProductsRequest;
import com.commerce.platform.order.service.domain.dto.client.SearchProductsResponse;

public interface ProductServiceClient {
    SearchProductsResponse searchProducts(SearchProductsRequest request);
} 