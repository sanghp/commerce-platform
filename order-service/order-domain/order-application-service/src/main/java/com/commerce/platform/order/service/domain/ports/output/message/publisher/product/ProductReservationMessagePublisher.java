package com.commerce.platform.order.service.domain.ports.output.message.publisher.product;

import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import com.commerce.platform.outbox.OutboxStatus;

import java.util.function.BiConsumer;

public interface ProductReservationMessagePublisher {

    default void publish(ProductReservationOutboxMessage reservationOutboxMessage,
                         BiConsumer<ProductReservationOutboxMessage, OutboxStatus> outboxCallback) {

    }
}
