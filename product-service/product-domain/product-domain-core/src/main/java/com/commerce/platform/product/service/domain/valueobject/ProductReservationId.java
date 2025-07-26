package com.commerce.platform.product.service.domain.valueobject;

import com.commerce.platform.domain.valueobject.BaseId;

import java.util.UUID;

public class ProductReservationId extends BaseId<UUID> {
    public ProductReservationId(UUID value) {
        super(value);
    }
} 