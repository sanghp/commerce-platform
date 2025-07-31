package com.commerce.platform.order.service.domain.ports.output.message.publisher.payment;

import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;

public interface PaymentRequestMessagePublisher {

    void publish(OrderOutboxMessage orderOutboxMessage);
}
