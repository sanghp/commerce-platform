package com.commerce.platform.payment.service.domain.valueobject;

import com.commerce.platform.domain.valueobject.BaseId;

import java.util.UUID;

public class CreditId extends BaseId<UUID> {
    public CreditId(UUID value) {
        super(value);
    }
}
