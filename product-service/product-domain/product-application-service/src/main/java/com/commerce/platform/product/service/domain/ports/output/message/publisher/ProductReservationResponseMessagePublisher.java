package com.commerce.platform.product.service.domain.ports.output.message.publisher;

import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.outbox.OutboxStatus;

import java.util.function.BiConsumer;

public interface ProductReservationResponseMessagePublisher {

    default void publish(ProductOutboxMessage outboxMessage,
                         BiConsumer<ProductOutboxMessage, OutboxStatus> outboxCallback) {

    }
} 