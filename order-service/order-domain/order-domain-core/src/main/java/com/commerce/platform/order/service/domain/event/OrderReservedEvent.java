package com.commerce.platform.order.service.domain.event;

import com.commerce.platform.order.service.domain.entity.Order;

import java.time.ZonedDateTime;

public class OrderReservedEvent extends OrderEvent {
    public OrderReservedEvent(Order order, ZonedDateTime createdAt) {
        super(order, createdAt);
    }
}
