package com.commerce.platform.product.service.domain.ports.output.message.publisher;

import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;

public interface ProductReservationResponseMessagePublisher {

    void publish(ProductOutboxMessage outboxMessage);
} 