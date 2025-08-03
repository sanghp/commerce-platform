package com.commerce.platform.payment.service.domain.ports.output.message.publisher;

import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;

public interface PaymentResponseMessagePublisher {
    void publish(PaymentOutboxMessage paymentOutboxMessage);
}