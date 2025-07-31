package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.order.service.domain.entity.Order;
import com.commerce.platform.order.service.domain.event.OrderReservedEvent;
import com.commerce.platform.order.service.domain.mapper.OrderDataMapper;
import com.commerce.platform.order.service.domain.outbox.scheduler.OrderOutboxHelper;
import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import com.commerce.platform.saga.SagaStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
public class ProductReservationSaga implements SagaStep<ProductReservationResponse> {

    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;
    private final OrderOutboxHelper orderOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    public ProductReservationSaga(OrderDomainService orderDomainService,
                                  OrderSagaHelper orderSagaHelper,
                                  OrderOutboxHelper orderOutboxHelper,
                                  OrderDataMapper orderDataMapper) {
        this.orderDomainService = orderDomainService;
        this.orderSagaHelper = orderSagaHelper;
        this.orderOutboxHelper = orderOutboxHelper;
        this.orderDataMapper = orderDataMapper;
    }

    @Override
    @Transactional
    public void process(ProductReservationResponse reservationResponse) {
        if (reservationResponse.getProductReservationStatus() == ProductReservationStatus.APPROVED) {
            OrderReservedEvent domainEvent = reserveOrder(reservationResponse);
            Order order = domainEvent.getOrder();

            SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getOrderStatus());
            
            orderOutboxHelper.saveOrderOutboxMessage(
                    ServiceMessageType.PAYMENT_REQUEST,
                    orderDataMapper.orderReservedEventToPaymentEventPayload(domainEvent),
                    OutboxStatus.STARTED,
                    reservationResponse.getSagaId());

            log.info("Order with id: {} is reserved", order.getId().getValue());
            
        } else if (reservationResponse.getProductReservationStatus() == ProductReservationStatus.BOOKED) {
            Order order = confirmOrderReservation(reservationResponse);
            log.info("Order reservation is confirmed for order id: {}", order.getId().getValue());
            
        } else {
            log.error("Unexpected product reservation status: {} for order id: {}",
                    reservationResponse.getProductReservationStatus(), 
                    reservationResponse.getOrderId());
        }
    }

    private Order confirmOrderReservation(ProductReservationResponse reservationResponse) {
        log.info("Booking product with id: {}", reservationResponse.getOrderId());
        Order order = orderSagaHelper.findOrder(reservationResponse.getOrderId());
        orderDomainService.confirmOrderReservation(order);
        orderSagaHelper.saveOrder(order);

        return order;
    }

    @Override
    @Transactional
    public void rollback(ProductReservationResponse reservationResponse) {
        Order order = rollbackOrder(reservationResponse);
        log.info("Order with id: {} is cancelled", order.getId().getValue());
    }

    private OrderReservedEvent reserveOrder(ProductReservationResponse reservationResponse) {
        log.info("Reserving product with id: {}", reservationResponse.getOrderId());
        Order order = orderSagaHelper.findOrder(reservationResponse.getOrderId());
        OrderReservedEvent domainEvent = orderDomainService.reserveOrder(order);
        orderSagaHelper.saveOrder(order);

        return domainEvent;
    }


    private Order rollbackOrder(ProductReservationResponse reservationResponse) {
        log.info("Cancelling order with id: {}", reservationResponse.getOrderId());
        Order order = orderSagaHelper.findOrder(reservationResponse.getOrderId());
        orderDomainService.cancelOrder(order, reservationResponse.getFailureMessages());
        orderSagaHelper.saveOrder(order);

        return order;
    }
}
