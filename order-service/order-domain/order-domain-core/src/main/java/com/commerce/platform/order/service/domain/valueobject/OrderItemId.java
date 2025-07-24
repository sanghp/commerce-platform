package com.commerce.platform.order.service.domain.valueobject;

import com.commerce.platform.domain.valueobject.BaseId;

public class OrderItemId extends BaseId<Long> {
    public OrderItemId(Long value) {
        super(value);
    }
}
