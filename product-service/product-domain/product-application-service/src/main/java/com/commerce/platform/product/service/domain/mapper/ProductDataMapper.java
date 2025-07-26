package com.commerce.platform.product.service.domain.mapper;

import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.product.service.domain.dto.create.CreateProductCommand;
import com.commerce.platform.product.service.domain.dto.create.CreateProductResponse;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.dto.query.SearchProductsResponse;
import com.commerce.platform.product.service.domain.dto.query.ProductResponse;
import com.commerce.platform.product.service.domain.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductDataMapper {

    public Product createProductCommandToProduct(CreateProductCommand createProductCommand) {
        return Product.builder()
                .productId(new ProductId(UUID.randomUUID()))
                .name(createProductCommand.getName())
                .price(new Money(createProductCommand.getPrice()))
                .quantity(createProductCommand.getQuantity())
                .build();
    }

    public CreateProductResponse productToCreateProductResponse(Product product, String message) {
        return CreateProductResponse.builder()
                .productId(product.getId().getValue())
                .message(message)
                .build();
    }

    public SearchProductsResponse productsToSearchProductsResponse(List<Product> products) {
        return SearchProductsResponse.builder()
                .products(products.stream()
                        .map(this::productToProductResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public ProductResponse productToProductResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getId().getValue())
                .name(product.getName())
                .price(product.getPrice().getAmount())
                .quantity(product.getQuantity())
                .enabled(product.isEnabled())
                .createdAt(product.getCreatedAt())
                .build();
    }
}