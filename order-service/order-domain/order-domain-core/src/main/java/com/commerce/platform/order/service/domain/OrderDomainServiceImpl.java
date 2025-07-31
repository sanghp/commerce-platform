package com.commerce.platform.order.service.domain;

import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.order.service.domain.entity.Order;
import com.commerce.platform.order.service.domain.entity.Product;
import com.commerce.platform.order.service.domain.event.OrderCancelledEvent;
import com.commerce.platform.order.service.domain.event.OrderCreatedEvent;
import com.commerce.platform.order.service.domain.event.OrderPaidEvent;
import com.commerce.platform.order.service.domain.event.OrderReservedEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class OrderDomainServiceImpl implements OrderDomainService {
    @Override
    public OrderCreatedEvent initiateOrder(Order order, List<Product> products) {
        setOrderProductInformation(order, products);
        order.validateOrder();
        order.initializeOrder();

        log.info("Order with id: {} is initiated", order.getId().getValue());

        return new OrderCreatedEvent(order, ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public OrderReservedEvent reserveOrder(Order order) {
        order.reserve();

        log.info("Order with id: {} is reserved", order.getId().getValue());

        return new OrderReservedEvent(order, ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public OrderPaidEvent payOrder(Order order) {
        order.pay();

        log.info("Order with id: {} is paid", order.getId().getValue());

        return new OrderPaidEvent(order, ZonedDateTime.now(ZoneOffset.UTC));
    }


    @Override
    public void confirmOrderReservation(Order order) {
        order.confirm();

        log.info("Order with id: {} is confirmed", order.getId().getValue());
    }

    @Override
    public OrderCancelledEvent cancelOrderReservation(Order order, List<String> failureMessages) {
        order.initCancel(failureMessages);
        log.info("Order reservation is cancelling for order id: {}", order.getId().getValue());
        return new OrderCancelledEvent(order, ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public void cancelOrder(Order order, List<String> failureMessages) {
        order.cancel(failureMessages);

        log.info("Order with id: {} is cancelled", order.getId().getValue());
    }

    private void setOrderProductInformation(Order order, List<Product> products) {
        Map<ProductId, Product> productMap = new HashMap<>();
        products.forEach(p -> productMap.put(p.getId(), p));

        order.getItems().forEach(orderItem -> {
            Product currentProduct = orderItem.getProduct();
            Product originProduct = productMap.get(currentProduct.getId());

            if (originProduct != null) {
                currentProduct.updateWithConfirmedNameAndPrice(originProduct.getName(), originProduct.getPrice());
            }
        });
    }
}
