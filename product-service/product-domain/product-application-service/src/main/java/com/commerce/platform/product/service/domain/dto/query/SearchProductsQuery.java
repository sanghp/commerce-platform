package com.commerce.platform.product.service.domain.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchProductsQuery {

    @NotEmpty(message = "Product IDs list cannot be empty")
    @Size(max = 100, message = "Cannot request more than 100 products at once")
    private List<UUID> productIds;
} 