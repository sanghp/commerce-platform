package com.commerce.platform.payment.service.dataaccess.outbox.mapper;

import com.commerce.platform.payment.service.dataaccess.outbox.entity.PaymentOutboxEntity;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxDataAccessMapper {
    
    public PaymentOutboxEntity paymentOutboxMessageToOutboxEntity(PaymentOutboxMessage paymentOutboxMessage) {
        return PaymentOutboxEntity.builder()
                .id(paymentOutboxMessage.getId())
                .messageId(paymentOutboxMessage.getMessageId())
                .sagaId(paymentOutboxMessage.getSagaId())
                .createdAt(paymentOutboxMessage.getCreatedAt())
                .fetchedAt(paymentOutboxMessage.getFetchedAt())
                .processedAt(paymentOutboxMessage.getProcessedAt())
                .type(paymentOutboxMessage.getType())
                .payload(paymentOutboxMessage.getPayload())
                .outboxStatus(paymentOutboxMessage.getOutboxStatus())
                .version(paymentOutboxMessage.getVersion())
                .traceId(paymentOutboxMessage.getTraceId())
                .spanId(paymentOutboxMessage.getSpanId())
                .build();
    }
    
    public PaymentOutboxMessage outboxEntityToPaymentOutboxMessage(PaymentOutboxEntity paymentOutboxEntity) {
        return PaymentOutboxMessage.builder()
                .id(paymentOutboxEntity.getId())
                .messageId(paymentOutboxEntity.getMessageId())
                .sagaId(paymentOutboxEntity.getSagaId())
                .createdAt(paymentOutboxEntity.getCreatedAt())
                .fetchedAt(paymentOutboxEntity.getFetchedAt())
                .processedAt(paymentOutboxEntity.getProcessedAt())
                .type(paymentOutboxEntity.getType())
                .payload(paymentOutboxEntity.getPayload())
                .outboxStatus(paymentOutboxEntity.getOutboxStatus())
                .version(paymentOutboxEntity.getVersion())
                .traceId(paymentOutboxEntity.getTraceId())
                .spanId(paymentOutboxEntity.getSpanId())
                .build();
    }
}