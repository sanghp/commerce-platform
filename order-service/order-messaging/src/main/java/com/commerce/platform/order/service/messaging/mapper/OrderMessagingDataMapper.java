package com.commerce.platform.order.service.messaging.mapper;

import com.commerce.platform.kafka.order.avro.model.*;
import com.commerce.platform.order.service.domain.dto.message.PaymentResponse;
import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationEventPayload;
import org.springframework.stereotype.Component;
import com.commerce.platform.domain.util.UuidGenerator;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderMessagingDataMapper {

    public ProductReservationResponse
    productReservationResponseAvroModelToProductReservationResponse(ProductReservationResponseAvroModel
                                                                            productReservationResponseAvroModel) {
        return ProductReservationResponse.builder()
                .id(productReservationResponseAvroModel.getId())
                .sagaId(productReservationResponseAvroModel.getSagaId())
                .orderId(productReservationResponseAvroModel.getOrderId())
                .createdAt(productReservationResponseAvroModel.getCreatedAt())
                .productReservationStatus(com.commerce.platform.domain.valueobject.ProductReservationStatus.valueOf(
                        productReservationResponseAvroModel.getProductReservationStatus().name()))
                .failureMessages(productReservationResponseAvroModel.getFailureMessages())
                .build();
    }

    public ProductReservationRequestAvroModel
    productReservationEventToRequestAvroModel(UUID messageId, UUID sagaId, ProductReservationEventPayload
            reservationEventPayload) {
        return ProductReservationRequestAvroModel.newBuilder()
                .setId(messageId)
                .setSagaId(sagaId)
                .setReservationOrderStatus(com.commerce.platform.kafka.order.avro.model.ProductReservationOrderStatus
                        .valueOf(reservationEventPayload.getReservationOrderStatus().name()))
                .setOrderId(reservationEventPayload.getOrderId())
                .setProducts(reservationEventPayload.getProducts().stream().map(product ->
                        com.commerce.platform.kafka.order.avro.model.Product.newBuilder()
                                .setId(product.getId())
                                .setQuantity(product.getQuantity())
                                .build()).collect(Collectors.toList()))
                .setPrice(reservationEventPayload.getPrice())
                .setCreatedAt(reservationEventPayload.getCreatedAt().toInstant())
                .build();
    }

    public PaymentResponse paymentResponseAvroModelToPaymentResponse(PaymentResponseAvroModel
                                                                             paymentResponseAvroModel) {
        return PaymentResponse.builder()
                .id(paymentResponseAvroModel.getId())
                .sagaId(paymentResponseAvroModel.getSagaId())
                .paymentId(paymentResponseAvroModel.getPaymentId())
                .customerId(paymentResponseAvroModel.getCustomerId())
                .orderId(paymentResponseAvroModel.getOrderId())
                .price(paymentResponseAvroModel.getPrice())
                .createdAt(paymentResponseAvroModel.getCreatedAt())
                .paymentStatus(com.commerce.platform.domain.valueobject.PaymentStatus.valueOf(
                        paymentResponseAvroModel.getPaymentStatus().name()))
                .failureMessages(paymentResponseAvroModel.getFailureMessages())
                .build();
    }

    public PaymentRequestAvroModel orderPaymentEventToPaymentRequestAvroModel(UUID messageId, UUID sagaId, OrderPaymentEventPayload
            orderPaymentEventPayload) {
        return PaymentRequestAvroModel.newBuilder()
                .setId(messageId)
                .setSagaId(sagaId)
                .setCustomerId(orderPaymentEventPayload.getCustomerId())
                .setOrderId(orderPaymentEventPayload.getOrderId())
                .setPrice(orderPaymentEventPayload.getPrice())
                .setCreatedAt(orderPaymentEventPayload.getCreatedAt().toInstant())
                .setPaymentOrderStatus(PaymentOrderStatus.valueOf(orderPaymentEventPayload.getPaymentOrderStatus()))
                .build();
    }
}
