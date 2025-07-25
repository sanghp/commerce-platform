package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.create.CreateOrderCommand;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderResponse;
import com.commerce.platform.order.service.domain.entity.Product;
import com.commerce.platform.order.service.domain.event.OrderCreatedEvent;
import com.commerce.platform.order.service.domain.mapper.OrderDataMapper;
import com.commerce.platform.order.service.domain.outbox.scheduler.product.ProductReservationOutboxHelper;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class OrderCreateCommandHandler {

    private final OrderCreateHelper orderCreateHelper;
    private final OrderDataMapper orderDataMapper;
    private final ProductReservationOutboxHelper reservationOutboxHelper;
    private final OrderSagaHelper orderSagaHelper;

    public OrderCreateCommandHandler(OrderCreateHelper orderCreateHelper,
                                     OrderDataMapper orderDataMapper,
                                     ProductReservationOutboxHelper reservationOutboxHelper,
                                     OrderSagaHelper orderSagaHelper) {
        this.orderCreateHelper = orderCreateHelper;
        this.orderDataMapper = orderDataMapper;
        this.reservationOutboxHelper = reservationOutboxHelper;
        this.orderSagaHelper = orderSagaHelper;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderCommand createOrderCommand, List<Product> products) {
        OrderCreatedEvent orderCreatedEvent = orderCreateHelper.persistOrder(createOrderCommand, products);
        log.info("Order is created with id: {}", orderCreatedEvent.getOrder().getId().getValue());
        CreateOrderResponse createOrderResponse = orderDataMapper.orderToCreateOrderResponse(orderCreatedEvent.getOrder(),
                "Order created successfully");

        reservationOutboxHelper.saveProductReservationOutboxMessage(orderDataMapper
                .orderCreatedEventToProductReservationEventPayload(orderCreatedEvent),
                orderCreatedEvent.getOrder().getOrderStatus(),
                orderSagaHelper.orderStatusToSagaStatus(orderCreatedEvent.getOrder().getOrderStatus()),
                OutboxStatus.STARTED,
                UUID.randomUUID());

        log.info("Returning CreateOrderResponse with order id: {}", orderCreatedEvent.getOrder().getId());

        return createOrderResponse;
    }
}
