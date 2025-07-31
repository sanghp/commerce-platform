package com.commerce.platform.order.service.domain.ports.output.message.publisher.product;

import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.outbox.OutboxStatus;

import java.util.function.BiConsumer;

public interface ProductReservationRequestMessagePublisher {

    void publish(OrderOutboxMessage orderOutboxMessage,
                 BiConsumer<OrderOutboxMessage, OutboxStatus> outboxCallback);
}