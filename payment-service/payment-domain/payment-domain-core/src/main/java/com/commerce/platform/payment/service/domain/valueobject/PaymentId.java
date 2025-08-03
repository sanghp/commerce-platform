package com.commerce.platform.payment.service.domain.valueobject;

import com.commerce.platform.domain.valueobject.BaseId;

import java.util.UUID;

public class PaymentId extends BaseId<UUID> {
    public PaymentId(UUID value) {
        super(value);
    }
}
