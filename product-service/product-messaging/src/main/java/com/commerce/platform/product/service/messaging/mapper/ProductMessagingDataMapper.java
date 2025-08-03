package com.commerce.platform.product.service.messaging.mapper;

import com.commerce.platform.kafka.order.avro.model.ProductReservationRequestAvroModel;
import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;
import com.commerce.platform.kafka.order.avro.model.ProductReservationStatus;
import com.commerce.platform.product.service.domain.dto.message.ProductDTO;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.valueobject.ProductReservationOrderStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductMessagingDataMapper {
    public ProductReservationRequest productReservationRequestAvroModelToProductReservation(
            ProductReservationRequestAvroModel reservationRequestAvroModel
    ) {
        return ProductReservationRequest.builder()
                .id(reservationRequestAvroModel.getId())
                .sagaId(reservationRequestAvroModel.getSagaId())
                .orderId(reservationRequestAvroModel.getOrderId())
                .reservationOrderStatus(ProductReservationOrderStatus.valueOf(reservationRequestAvroModel.getReservationOrderStatus().name()))
                .products(reservationRequestAvroModel.getProducts().stream().map(avroModel ->
                        ProductDTO.builder()
                                .productId(avroModel.getId())
                                .quantity(avroModel.getQuantity())
                                .build()).collect(Collectors.toList())
                )
                .price(reservationRequestAvroModel.getPrice())
                .createdAt(reservationRequestAvroModel.getCreatedAt())
                .build();
    }

    public ProductReservationResponseAvroModel productReservationResponseEventToResponseAvroModel(
            UUID messageId,
            UUID sagaId,
            ProductReservationResponseEventPayload responseEventPayload
    ) {
        return ProductReservationResponseAvroModel.newBuilder()
                .setId(messageId)
                .setSagaId(sagaId)
                .setOrderId(responseEventPayload.getOrderId())
                .setProductReservationStatus(
                    ProductReservationStatus.valueOf(responseEventPayload.getReservationStatus())
                )
                .setProducts(responseEventPayload.getProducts().stream()
                        .map(product -> com.commerce.platform.kafka.order.avro.model.Product.newBuilder()
                                .setId(product.getId())
                                .setQuantity(product.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .setFailureMessages(responseEventPayload.getFailureMessages())
                .setCreatedAt(responseEventPayload.getCreatedAt().toInstant())
                .build();
    }
}
