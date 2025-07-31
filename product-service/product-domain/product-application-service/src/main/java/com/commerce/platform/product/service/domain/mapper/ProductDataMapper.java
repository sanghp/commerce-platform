package com.commerce.platform.product.service.domain.mapper;

import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.product.service.domain.dto.create.CreateProductCommand;
import com.commerce.platform.product.service.domain.dto.create.CreateProductResponse;
import com.commerce.platform.product.service.domain.dto.message.ProductDTO;
import com.commerce.platform.product.service.domain.dto.query.SearchProductsResponse;
import com.commerce.platform.product.service.domain.dto.query.ProductResponse;
import com.commerce.platform.product.service.domain.entity.Product;
import org.springframework.stereotype.Component;
import com.commerce.platform.domain.util.UuidGenerator;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductDataMapper {

    public Product createProductCommandToProduct(CreateProductCommand createProductCommand) {
        return Product.builder()
                .productId(new ProductId(UuidGenerator.generate()))
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

    public Product productDTOToProduct(ProductDTO productDTO) {
        return Product.builder()
                .productId(new ProductId(productDTO.getProductId()))
                .name(productDTO.getName())
                .price(productDTO.getPrice() != null ? new Money(productDTO.getPrice()) : null)
                .quantity(productDTO.getQuantity())
                .reservedQuantity(productDTO.getReservedQuantity() != null ? productDTO.getReservedQuantity() : 0)
                .enabled(productDTO.isEnabled())
                .build();
    }

    public List<Product> productDTOsToProducts(List<ProductDTO> productDTOs) {
        return productDTOs.stream()
                .map(this::productDTOToProduct)
                .collect(Collectors.toList());
    }
}