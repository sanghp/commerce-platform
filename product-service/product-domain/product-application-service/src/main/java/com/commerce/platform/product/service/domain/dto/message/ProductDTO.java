package com.commerce.platform.product.service.domain.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private UUID productId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private Integer reservedQuantity;
    private boolean enabled;
}