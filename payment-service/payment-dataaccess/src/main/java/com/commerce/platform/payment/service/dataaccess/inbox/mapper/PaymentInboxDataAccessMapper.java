package com.commerce.platform.payment.service.dataaccess.inbox.mapper;

import com.commerce.platform.payment.service.dataaccess.inbox.entity.PaymentInboxEntity;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import org.springframework.stereotype.Component;

@Component
public class PaymentInboxDataAccessMapper {
    
    public PaymentInboxEntity paymentInboxMessageToInboxEntity(PaymentInboxMessage paymentInboxMessage) {
        return PaymentInboxEntity.builder()
                .id(paymentInboxMessage.getId())
                .messageId(paymentInboxMessage.getMessageId())
                .sagaId(paymentInboxMessage.getSagaId())
                .type(paymentInboxMessage.getType())
                .payload(paymentInboxMessage.getPayload())
                .status(paymentInboxMessage.getStatus())
                .receivedAt(paymentInboxMessage.getReceivedAt())
                .processedAt(paymentInboxMessage.getProcessedAt())
                .retryCount(paymentInboxMessage.getRetryCount())
                .errorMessage(paymentInboxMessage.getErrorMessage())
                .traceId(paymentInboxMessage.getTraceId())
                .spanId(paymentInboxMessage.getSpanId())
                .build();
    }
    
    public PaymentInboxMessage inboxEntityToPaymentInboxMessage(PaymentInboxEntity paymentInboxEntity) {
        return PaymentInboxMessage.builder()
                .id(paymentInboxEntity.getId())
                .messageId(paymentInboxEntity.getMessageId())
                .sagaId(paymentInboxEntity.getSagaId())
                .type(paymentInboxEntity.getType())
                .payload(paymentInboxEntity.getPayload())
                .status(paymentInboxEntity.getStatus())
                .receivedAt(paymentInboxEntity.getReceivedAt())
                .processedAt(paymentInboxEntity.getProcessedAt())
                .retryCount(paymentInboxEntity.getRetryCount())
                .errorMessage(paymentInboxEntity.getErrorMessage())
                .traceId(paymentInboxEntity.getTraceId())
                .spanId(paymentInboxEntity.getSpanId())
                .build();
    }
}