package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.client.SearchProductsRequest;
import com.commerce.platform.order.service.domain.dto.client.SearchProductsResponse;
import com.commerce.platform.order.service.domain.ports.output.client.ProductServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductServiceClientImpl implements ProductServiceClient {

    private static final String PRODUCT_SEARCH_PATH = "/api/v1/products/search";

    private final WebClient webClient;
    private final String productServiceUrl;

    public ProductServiceClientImpl(WebClient webClient,
                                   @Value("${product-service.url}") String productServiceUrl) {
        this.webClient = webClient;
        this.productServiceUrl = productServiceUrl;
    }

    @Override
    @Cacheable(value = "products", key = "#request.productIds.toString()", 
               condition = "#request.productIds.size() <= 10",
               unless = "#result == null || #result.products.isEmpty()")
    public SearchProductsResponse searchProducts(SearchProductsRequest request) {
        log.info("Calling Product Service to get products for IDs: {}", request.getProductIds());
        
        String url = productServiceUrl + PRODUCT_SEARCH_PATH;
        
        try {
            SearchProductsResponse response = webClient
                    .post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SearchProductsResponse.class)
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                            .maxBackoff(Duration.ofSeconds(1)))
                    .timeout(Duration.ofSeconds(3))
                    .onErrorResume(error -> {
                        log.error("Error calling Product Service: {}", error.getMessage());
                        return Mono.empty();
                    })
                    .block();
            
            log.info("Received {} products from Product Service", 
                    response != null ? response.getProducts().size() : 0);
            
            return response;
        } catch (Exception e) {
            log.error("Failed to call Product Service", e);
            throw new RuntimeException("Product service unavailable", e);
        }
    }
}