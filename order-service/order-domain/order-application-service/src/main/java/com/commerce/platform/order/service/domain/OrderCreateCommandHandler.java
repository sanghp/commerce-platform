package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.create.CreateOrderCommand;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderResponse;
import com.commerce.platform.order.service.domain.entity.Product;
import com.commerce.platform.order.service.domain.event.OrderCreatedEvent;
import com.commerce.platform.order.service.domain.mapper.OrderDataMapper;
import com.commerce.platform.order.service.domain.outbox.scheduler.OrderOutboxHelper;
import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.domain.util.UuidGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class OrderCreateCommandHandler {

    private final OrderCreateHelper orderCreateHelper;
    private final OrderDataMapper orderDataMapper;
    private final OrderOutboxHelper orderOutboxHelper;

    public OrderCreateCommandHandler(OrderCreateHelper orderCreateHelper,
                                     OrderDataMapper orderDataMapper,
                                     OrderOutboxHelper orderOutboxHelper) {
        this.orderCreateHelper = orderCreateHelper;
        this.orderDataMapper = orderDataMapper;
        this.orderOutboxHelper = orderOutboxHelper;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderCommand createOrderCommand, List<Product> products) {
        OrderCreatedEvent orderCreatedEvent = orderCreateHelper.persistOrder(createOrderCommand, products);
        log.info("Order is created with id: {}", orderCreatedEvent.getOrder().getId().getValue());
        CreateOrderResponse createOrderResponse = orderDataMapper.orderToCreateOrderResponse(orderCreatedEvent.getOrder(),
                "Order created successfully");

        orderOutboxHelper.saveOrderOutboxMessage(
                ServiceMessageType.PRODUCT_RESERVATION_REQUEST,
                orderDataMapper.orderCreatedEventToProductReservationEventPayload(orderCreatedEvent),
                OutboxStatus.STARTED,
                UuidGenerator.generate());

        log.info("Returning CreateOrderResponse with order id: {}", orderCreatedEvent.getOrder().getId());

        return createOrderResponse;
    }
}
