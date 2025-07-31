package com.commerce.platform.product.service.application.rest;

import com.commerce.platform.product.service.domain.dto.create.CreateProductCommand;
import com.commerce.platform.product.service.domain.dto.create.CreateProductResponse;
import com.commerce.platform.product.service.domain.dto.query.SearchProductsQuery;
import com.commerce.platform.product.service.domain.dto.query.SearchProductsResponse;
import com.commerce.platform.product.service.domain.ports.input.service.ProductApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/products")
@Tag(name = "Products", description = "Product API")
public class ProductController {

    private final ProductApplicationService productApplicationService;

    public ProductController(ProductApplicationService productApplicationService) {
        this.productApplicationService = productApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create a new product", description = "Creates a new product with specified details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data")
    })
    public ResponseEntity<CreateProductResponse> createProduct(@Valid @RequestBody CreateProductCommand createProductCommand) {
        log.info("Creating product with name: {}", createProductCommand.getName());
        CreateProductResponse createProductResponse = productApplicationService.createProduct(createProductCommand);
        log.info("Product created with id: {}", createProductResponse.getProductId());
        return ResponseEntity.ok(createProductResponse);
    }

    @PostMapping("/search")
    @Operation(summary = "Search products by IDs", description = "Get product details for multiple product IDs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product IDs")
    })
    public ResponseEntity<SearchProductsResponse> searchProducts(
            @Valid @RequestBody SearchProductsQuery searchProductsQuery) {
        log.info("Searching products for {} product IDs", searchProductsQuery.getProductIds().size());
        SearchProductsResponse searchProductsResponse = productApplicationService.searchProducts(searchProductsQuery);
        log.info("Found {} products", searchProductsResponse.getProducts().size());
        return ResponseEntity.ok(searchProductsResponse);
    }
} 