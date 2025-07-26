package com.commerce.platform.product.service.domain.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductReservationProduct {
    private final UUID id;
    private final Integer quantity;
} 