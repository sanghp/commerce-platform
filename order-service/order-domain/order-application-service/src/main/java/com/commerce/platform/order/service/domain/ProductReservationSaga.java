package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.domain.valueobject.OrderStatus;
import com.commerce.platform.order.service.domain.entity.Order;
import com.commerce.platform.order.service.domain.event.OrderReservedEvent;
import com.commerce.platform.order.service.domain.exception.OrderDomainException;
import com.commerce.platform.order.service.domain.mapper.OrderDataMapper;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import com.commerce.platform.order.service.domain.outbox.scheduler.product.ProductReservationOutboxHelper;
import com.commerce.platform.order.service.domain.outbox.scheduler.payment.PaymentOutboxHelper;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import com.commerce.platform.saga.SagaStep;
import org.springframework.dao.OptimisticLockingFailureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ProductReservationSaga implements SagaStep<ProductReservationResponse> {

    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final ProductReservationOutboxHelper reservationOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    public ProductReservationSaga(OrderDomainService orderDomainService,
                                  OrderSagaHelper orderSagaHelper,
                                  PaymentOutboxHelper paymentOutboxHelper,
                                  ProductReservationOutboxHelper reservationOutboxHelper,
                                  OrderDataMapper orderDataMapper) {
        this.orderDomainService = orderDomainService;
        this.orderSagaHelper = orderSagaHelper;
        this.paymentOutboxHelper = paymentOutboxHelper;
        this.reservationOutboxHelper = reservationOutboxHelper;
        this.orderDataMapper = orderDataMapper;
    }

    @Override
    @Transactional
    public void process(ProductReservationResponse reservationResponse) {
        Optional<ProductReservationOutboxMessage> reservationOutboxMessageResponse =
                reservationOutboxHelper.getProductReservationOutboxMessageBySagaIdAndSagaStatus(
                        reservationResponse.getSagaId(),
                        SagaStatus.STARTED, SagaStatus.PROCESSING);

        if (reservationOutboxMessageResponse.isEmpty()) {
            log.info("An outbox message with saga id: {} is already processed!",
                    reservationResponse.getSagaId());
            return;
        }

        ProductReservationOutboxMessage reservationOutboxMessage = reservationOutboxMessageResponse.get();
        
        try {
            Order order;
            SagaStatus sagaStatus;

            if (reservationOutboxMessage.getOrderStatus() == OrderStatus.PENDING) {
                OrderReservedEvent domainEvent = reserveOrder(reservationResponse);
                order = domainEvent.getOrder();

                sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getOrderStatus());
                paymentOutboxHelper.savePaymentOutboxMessage(orderDataMapper
                                .orderReservedEventToPaymentEventPayload(domainEvent),
                        order.getOrderStatus(),
                        sagaStatus,
                        OutboxStatus.STARTED,
                        reservationResponse.getSagaId());

                log.info("Order with id: {} is reserved", order.getId().getValue());
            } else if (reservationOutboxMessage.getOrderStatus() == OrderStatus.CONFIRMED) {
                order = confirmOrderReservation(reservationResponse);
                sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getOrderStatus());

                log.info("Order with id: {} is confirmed", order.getId().getValue());
            } else {
                log.error("Order is not in PENDING state for reservation with saga id: {}",
                        reservationResponse.getSagaId());
                throw new OrderDomainException("Order is not in PENDING or CONFIRMED state for reservation with saga id: " +
                        reservationResponse.getSagaId());
            }

            reservationOutboxHelper.save(getUpdatedProductReservationOutboxMessage(reservationOutboxMessage.getSagaId(),
                    order.getOrderStatus(), sagaStatus));
                    
        } catch (OptimisticLockingFailureException e) {
            log.info("Concurrent processing detected for saga id: {}. Another thread may have processed this request.",
                    reservationResponse.getSagaId());
            return;
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
        Optional<ProductReservationOutboxMessage> reservationOutboxMessageResponse =
                reservationOutboxHelper.getProductReservationOutboxMessageBySagaIdAndSagaStatus(
                        reservationResponse.getSagaId(),
                        SagaStatus.STARTED, SagaStatus.COMPENSATING);

        if (reservationOutboxMessageResponse.isEmpty()) {
            log.info("An outbox message with saga id: {} is already roll backed!",
                    reservationResponse.getSagaId());
            return;
        }

        ProductReservationOutboxMessage reservationOutboxMessage = reservationOutboxMessageResponse.get();

        try {
            Order order = rollbackOrder(reservationResponse);

            SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getOrderStatus());

            reservationOutboxHelper.save(getUpdatedProductReservationOutboxMessage(reservationOutboxMessage.getSagaId(),
                    order.getOrderStatus(), sagaStatus));

            log.info("Order with id: {} is cancelled", order.getId().getValue());
        } catch (OptimisticLockingFailureException e) {
            log.info("Concurrent rollback detected for saga id: {}. Another thread may have processed this request.",
                    reservationResponse.getSagaId());
            return;
        }
    }

    private OrderReservedEvent reserveOrder(ProductReservationResponse reservationResponse) {
        log.info("Reserving product with id: {}", reservationResponse.getOrderId());
        Order order = orderSagaHelper.findOrder(reservationResponse.getOrderId());
        OrderReservedEvent domainEvent = orderDomainService.reserveOrder(order);
        orderSagaHelper.saveOrder(order);

        return domainEvent;
    }

    private ProductReservationOutboxMessage getUpdatedProductReservationOutboxMessage(UUID sagaId,
                                                                                OrderStatus orderStatus,
                                                                                SagaStatus sagaStatus) {
        Optional<ProductReservationOutboxMessage> reservationOutboxMessageResponse = reservationOutboxHelper
                .getProductReservationOutboxMessageBySagaIdAndSagaStatus(sagaId, SagaStatus.STARTED, SagaStatus.COMPENSATING);
        if (reservationOutboxMessageResponse.isEmpty()) {
            throw new OrderDomainException("Product Reservation outbox message cannot be found in " +
                    SagaStatus.STARTED.name() + " or " + SagaStatus.COMPENSATING.name() + " state");
        }
        ProductReservationOutboxMessage reservedOutboxMessage = reservationOutboxMessageResponse.get();
        reservedOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneOffset.UTC));
        reservedOutboxMessage.setSagaStatus(sagaStatus);

        return reservedOutboxMessage;
    }

    private Order rollbackOrder(ProductReservationResponse reservationResponse) {
        log.info("Cancelling order with id: {}", reservationResponse.getOrderId());
        Order order = orderSagaHelper.findOrder(reservationResponse.getOrderId());
        orderDomainService.cancelOrder(order, reservationResponse.getFailureMessages());
        orderSagaHelper.saveOrder(order);

        return order;
    }
}
