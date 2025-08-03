package com.commerce.platform.payment.service.domain.mapper;

import com.commerce.platform.domain.valueobject.CustomerId;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.payment.service.domain.dto.PaymentRequest;
import com.commerce.platform.payment.service.domain.dto.PaymentResponse;
import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.event.PaymentEvent;
import com.commerce.platform.payment.service.domain.event.PaymentCompletedEvent;
import com.commerce.platform.payment.service.domain.event.PaymentFailedEvent;
import com.commerce.platform.payment.service.domain.event.PaymentCancelledEvent;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class PaymentDataMapper {
    
    private final ObjectMapper objectMapper;
    
    public PaymentDataMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public Payment paymentRequestToPayment(PaymentRequest paymentRequest) {
        return Payment.builder()
                .orderId(new OrderId(paymentRequest.getOrderId()))
                .customerId(new CustomerId(paymentRequest.getCustomerId()))
                .price(new Money(paymentRequest.getPrice()))
                .build();
    }
    
    public PaymentOutboxMessage paymentEventToPaymentOutboxMessage(PaymentEvent paymentEvent, 
                                                                  UUID sagaId) {
        return PaymentOutboxMessage.builder()
                .id(UUID.randomUUID())
                .messageId(UUID.randomUUID())
                .sagaId(sagaId)
                .type(mapEventTypeToMessageType(paymentEvent))
                .payload(createPayload(paymentEvent))
                .createdAt(paymentEvent.getCreatedAt())
                .build();
    }
    
    private ServiceMessageType mapEventTypeToMessageType(PaymentEvent paymentEvent) {
        if (paymentEvent instanceof PaymentCompletedEvent || 
            paymentEvent instanceof PaymentFailedEvent || 
            paymentEvent instanceof PaymentCancelledEvent) {
            return ServiceMessageType.PAYMENT_RESPONSE;
        }
        
        throw new IllegalArgumentException("Unknown payment event type: " + paymentEvent.getClass().getSimpleName());
    }
    
    public PaymentResponse paymentEventToPaymentResponse(PaymentEvent paymentEvent, UUID sagaId) {
        return PaymentResponse.builder()
                .id(sagaId)
                .sagaId(sagaId)
                .paymentId(paymentEvent.getPayment().getId().getValue())
                .customerId(paymentEvent.getPayment().getCustomerId().getValue())
                .orderId(paymentEvent.getPayment().getOrderId().getValue())
                .price(paymentEvent.getPayment().getPrice().getAmount())
                .createdAt(paymentEvent.getCreatedAt())
                .paymentStatus(paymentEvent.getPayment().getPaymentStatus())
                .failureMessages(paymentEvent.getFailureMessages())
                .build();
    }
    
    private String createPayload(PaymentEvent paymentEvent) {
        try {
            PaymentResponse paymentResponse = PaymentResponse.builder()
                    .paymentId(paymentEvent.getPayment().getId().getValue())
                    .customerId(paymentEvent.getPayment().getCustomerId().getValue())
                    .orderId(paymentEvent.getPayment().getOrderId().getValue())
                    .price(paymentEvent.getPayment().getPrice().getAmount())
                    .createdAt(paymentEvent.getCreatedAt())
                    .paymentStatus(paymentEvent.getPayment().getPaymentStatus())
                    .failureMessages(paymentEvent.getFailureMessages())
                    .build();
            
            return objectMapper.writeValueAsString(paymentResponse);
        } catch (JsonProcessingException e) {
            log.error("Could not create payload for payment event", e);
            throw new RuntimeException("Could not create payload for payment event", e);
        }
    }
}