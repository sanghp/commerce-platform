package com.commerce.platform.order.service.domain.event;

import com.commerce.platform.domain.event.DomainEvent;
import com.commerce.platform.order.service.domain.entity.Order;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public abstract class OrderEvent implements DomainEvent<Order> {
    private final Order order;
    private final ZonedDateTime createdAt;

    public OrderEvent(Order order, ZonedDateTime createdAt) {
        this.order = order;
        this.createdAt = createdAt;
    }
}
