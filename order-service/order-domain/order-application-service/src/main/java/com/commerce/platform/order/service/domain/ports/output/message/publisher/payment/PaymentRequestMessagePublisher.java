package com.commerce.platform.order.service.domain.ports.output.message.publisher.payment;

import com.commerce.platform.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.commerce.platform.outbox.OutboxStatus;

import java.util.function.BiConsumer;

public interface PaymentRequestMessagePublisher {

    void publish(OrderPaymentOutboxMessage orderPaymentOutboxMessage,
                 BiConsumer<OrderPaymentOutboxMessage, OutboxStatus> outboxCallback);
}
