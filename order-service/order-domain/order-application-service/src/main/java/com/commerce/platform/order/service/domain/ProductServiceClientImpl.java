package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.client.SearchProductsRequest;
import com.commerce.platform.order.service.domain.dto.client.SearchProductsResponse;
import com.commerce.platform.order.service.domain.ports.output.client.ProductServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ProductServiceClientImpl implements ProductServiceClient {

    private static final String PRODUCT_SEARCH_PATH = "/api/v1/products/search";

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductServiceClientImpl(RestTemplate restTemplate,
                                   @Value("${product-service.url}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    @Override
    public SearchProductsResponse searchProducts(SearchProductsRequest request) {
        log.info("Calling Product Service to get products for IDs: {}", request.getProductIds());
        
        String url = productServiceUrl + PRODUCT_SEARCH_PATH;
        SearchProductsResponse response = restTemplate.postForObject(url, request, SearchProductsResponse.class);
        
        log.info("Received {} products from Product Service", 
                response != null ? response.getProducts().size() : 0);
        
        return response;
    }
} 