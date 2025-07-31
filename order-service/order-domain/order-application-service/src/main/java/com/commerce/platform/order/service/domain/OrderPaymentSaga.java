package com.commerce.platform.order.service.domain;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.order.service.domain.dto.message.PaymentResponse;
import com.commerce.platform.order.service.domain.entity.Order;
import com.commerce.platform.order.service.domain.event.OrderCancelledEvent;
import com.commerce.platform.order.service.domain.event.OrderPaidEvent;
import com.commerce.platform.order.service.domain.exception.OrderNotFoundException;
import com.commerce.platform.order.service.domain.mapper.OrderDataMapper;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.outbox.scheduler.OrderOutboxHelper;
import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderRepository;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class OrderPaymentSaga implements SagaStep<PaymentResponse> {

    private final OrderDomainService orderDomainService;
    private final OrderRepository orderRepository;
    private final OrderOutboxHelper orderOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    public OrderPaymentSaga(OrderDomainService orderDomainService,
                            OrderRepository orderRepository,
                            OrderOutboxHelper orderOutboxHelper,
                            OrderDataMapper orderDataMapper) {
        this.orderDomainService = orderDomainService;
        this.orderRepository = orderRepository;
        this.orderOutboxHelper = orderOutboxHelper;
        this.orderDataMapper = orderDataMapper;
    }

    @Override
    @Transactional
    public void process(PaymentResponse paymentResponse) {
        Optional<OrderOutboxMessage> orderOutboxMessageResponse =
                orderOutboxHelper.getOrderOutboxMessageByTypeAndSagaIdAndOutboxStatus(
                        ServiceMessageType.PAYMENT_REQUEST.name(),
                        paymentResponse.getSagaId(),
                        OutboxStatus.STARTED);

        if (orderOutboxMessageResponse.isEmpty()) {
            log.info("An outbox message with saga id: {} is already processed!", paymentResponse.getSagaId());

            return;
        }

        OrderOutboxMessage orderOutboxMessage = orderOutboxMessageResponse.get();
        OrderPaidEvent domainEvent = completePaymentForOrder(paymentResponse);
        
        // Update payment outbox message status
        orderOutboxMessage.setOutboxStatus(OutboxStatus.COMPLETED);
        orderOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneOffset.UTC));
        orderOutboxHelper.save(orderOutboxMessage);

        // Save product reservation request
        orderOutboxHelper.saveOrderOutboxMessage(
                ServiceMessageType.PRODUCT_RESERVATION_REQUEST,
                orderDataMapper.orderPaidEventToProductReservationEventPayload(domainEvent),
                OutboxStatus.STARTED,
                paymentResponse.getSagaId());

        log.info("Order with id: {} is paid", domainEvent.getOrder().getId().getValue());
    }

    @Override
    @Transactional
    public void rollback(PaymentResponse paymentResponse) {
        Optional<OrderOutboxMessage> orderOutboxMessageResponse =
                orderOutboxHelper.getOrderOutboxMessageByTypeAndSagaIdAndOutboxStatus(
                        ServiceMessageType.PAYMENT_REQUEST.name(),
                        paymentResponse.getSagaId(),
                        OutboxStatus.STARTED);

        if (orderOutboxMessageResponse.isEmpty()) {
            log.info("An outbox message with saga id: {} is already roll backed!", paymentResponse.getSagaId());
            return;
        }

        OrderOutboxMessage orderOutboxMessage = orderOutboxMessageResponse.get();
        OrderCancelledEvent domainEvent = rollbackReservationForOrder(paymentResponse);
        
        // Update payment outbox message status
        orderOutboxMessage.setOutboxStatus(OutboxStatus.COMPLETED);
        orderOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneOffset.UTC));
        orderOutboxHelper.save(orderOutboxMessage);

        // Save product reservation cancellation request
        orderOutboxHelper.saveOrderOutboxMessage(
                ServiceMessageType.PRODUCT_RESERVATION_REQUEST,
                orderDataMapper.orderCancelledEventToOrderPaymentEventPayload(domainEvent),
                OutboxStatus.STARTED,
                paymentResponse.getSagaId());

        log.info("Order with id: {} is cancelled", domainEvent.getOrder().getId().getValue());
    }

    private Order findOrder(UUID orderId) {
        Optional<Order> orderResponse = orderRepository.findById(new OrderId(orderId));
        if (orderResponse.isEmpty()) {
            log.error("Order with id: {} could not be found!", orderId);
            throw new OrderNotFoundException("Order with id " + orderId + " could not be found!");
        }
        return orderResponse.get();
    }


    private OrderPaidEvent completePaymentForOrder(PaymentResponse paymentResponse) {
        log.info("Completing payment for order with id: {}", paymentResponse.getOrderId());
        Order order = findOrder(paymentResponse.getOrderId());
        OrderPaidEvent domainEvent = orderDomainService.payOrder(order);
        orderRepository.save(order);
        return domainEvent;
    }


    private OrderCancelledEvent rollbackReservationForOrder(PaymentResponse paymentResponse) {
        log.info("Cancelling order with id: {}", paymentResponse.getOrderId());
        Order order = findOrder(paymentResponse.getOrderId());
        OrderCancelledEvent domainEvent = orderDomainService.cancelOrderReservation(order, paymentResponse.getFailureMessages());
        orderRepository.save(order);
        return domainEvent;
    }
}
