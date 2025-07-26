package com.commerce.platform.product.service.domain.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductResponse {
    private final UUID productId;
    private final String name;
    private final BigDecimal price;
    private final Integer quantity;
    private final boolean enabled;
    private final ZonedDateTime createdAt;
} 