package com.commerce.platform.order.service.domain.ports.output.message.publisher.product;

import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;

public interface ProductReservationRequestMessagePublisher {

    void publish(OrderOutboxMessage orderOutboxMessage);
}