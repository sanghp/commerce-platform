package com.commerce.platform.order.service.domain.valueobject;

import com.commerce.platform.domain.valueobject.BaseId;

import java.util.UUID;

public class TrackingId extends BaseId<UUID> {
    public TrackingId(UUID value) {
        super(value);
    }
}
