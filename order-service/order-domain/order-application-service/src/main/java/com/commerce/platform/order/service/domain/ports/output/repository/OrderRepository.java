package com.commerce.platform.order.service.domain.ports.output.repository;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.order.service.domain.entity.Order;
import com.commerce.platform.order.service.domain.valueobject.TrackingId;

import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    Optional<Order> findByTrackingId(TrackingId trackingId);
}
