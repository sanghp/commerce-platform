package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.entity.Order;
import com.commerce.platform.order.service.domain.entity.Product;
import com.commerce.platform.order.service.domain.event.OrderCancelledEvent;
import com.commerce.platform.order.service.domain.event.OrderCreatedEvent;
import com.commerce.platform.order.service.domain.event.OrderPaidEvent;
import com.commerce.platform.order.service.domain.event.OrderReservedEvent;

import java.util.List;

public interface OrderDomainService {

    OrderCreatedEvent initiateOrder(Order order, List<Product> products);

    OrderReservedEvent reserveOrder(Order order);

    OrderPaidEvent payOrder(Order order);

    void confirmOrderReservation(Order order);

    OrderCancelledEvent cancelOrderReservation(Order order, List<String> failureMessages);

    void cancelOrder(Order order, List<String> failureMessages);

}
