package com.commerce.platform.product.service.domain.valueobject;

import com.commerce.platform.domain.valueobject.BaseId;

import java.util.UUID;

public class SagaId extends BaseId<UUID> {
    public SagaId(UUID value) {
        super(value);
    }
} 