package com.commerce.platform.payment.service.messaging.mapper;

import com.commerce.platform.kafka.order.avro.model.PaymentRequestAvroModel;
import com.commerce.platform.kafka.order.avro.model.PaymentResponseAvroModel;
import com.commerce.platform.kafka.order.avro.model.PaymentStatus;
import com.commerce.platform.payment.service.domain.dto.PaymentRequest;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentEventPayload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PaymentMessagingDataMapper {

    public PaymentRequest paymentRequestAvroModelToPaymentRequest(PaymentRequestAvroModel paymentRequestAvroModel) {
        return PaymentRequest.builder()
                .id(paymentRequestAvroModel.getId())
                .sagaId(paymentRequestAvroModel.getSagaId())
                .orderId(paymentRequestAvroModel.getOrderId())
                .customerId(paymentRequestAvroModel.getCustomerId())
                .price(paymentRequestAvroModel.getPrice())
                .createdAt(paymentRequestAvroModel.getCreatedAt())
                .paymentOrderStatus(com.commerce.platform.domain.valueobject.PaymentOrderStatus.valueOf(
                        paymentRequestAvroModel.getPaymentOrderStatus().name()))
                .build();
    }

    public List<PaymentRequest> paymentRequestAvroModelsToPaymentRequests(List<PaymentRequestAvroModel> paymentRequestAvroModels) {
        return paymentRequestAvroModels.stream()
                .map(this::paymentRequestAvroModelToPaymentRequest)
                .collect(Collectors.toList());
    }

    public PaymentResponseAvroModel paymentEventPayloadToPaymentResponseAvroModel(UUID messageId,
                                                                                  UUID sagaId,
                                                                                  PaymentEventPayload paymentEventPayload) {
        return PaymentResponseAvroModel.newBuilder()
                .setId(messageId)
                .setSagaId(sagaId)
                .setPaymentId(paymentEventPayload.getPaymentId())
                .setCustomerId(paymentEventPayload.getCustomerId())
                .setOrderId(paymentEventPayload.getOrderId())
                .setPrice(paymentEventPayload.getPrice())
                .setCreatedAt(paymentEventPayload.getCreatedAt().toInstant())
                .setPaymentStatus(PaymentStatus.valueOf(paymentEventPayload.getPaymentStatus()))
                .setFailureMessages(paymentEventPayload.getFailureMessages())
                .build();
    }
}