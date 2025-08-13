package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.client.SearchProductsRequest;
import com.commerce.platform.order.service.domain.dto.client.SearchProductsResponse;
import com.commerce.platform.order.service.domain.ports.output.client.ProductServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class ProductServiceClientImpl implements ProductServiceClient {

    private static final String PRODUCT_SEARCH_PATH = "/api/v1/products/search";

    private final RestClient restClient;
    private final String productServiceUrl;

    public ProductServiceClientImpl(RestClient restClient,
                                   @Value("${product-service.url}") String productServiceUrl) {
        this.restClient = restClient;
        this.productServiceUrl = productServiceUrl;
    }

    @Override
    public SearchProductsResponse searchProducts(SearchProductsRequest request) {
        log.info("Calling Product Service to get products for IDs: {}", request.getProductIds());
        
        String url = productServiceUrl + PRODUCT_SEARCH_PATH;
        
        try {
            SearchProductsResponse response = restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SearchProductsResponse.class);
            
            log.info("Received {} products from Product Service", 
                    response != null ? response.getProducts().size() : 0);
            
            return response;
        } catch (RestClientException e) {
            log.error("Failed to call Product Service: {}", e.getMessage());
            throw new RuntimeException("Product service unavailable", e);
        }
    }
}