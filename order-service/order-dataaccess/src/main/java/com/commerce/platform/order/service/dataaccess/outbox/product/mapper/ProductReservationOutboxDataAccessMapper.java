package com.commerce.platform.order.service.dataaccess.outbox.product.mapper;

import com.commerce.platform.order.service.dataaccess.outbox.product.entity.ProductReservationOutboxEntity;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import org.springframework.stereotype.Component;

@Component
public class ProductReservationOutboxDataAccessMapper {

    public ProductReservationOutboxEntity orderCreatedOutboxMessageToOutboxEntity(ProductReservationOutboxMessage
                                                                                reservationOutboxMessage) {
        return ProductReservationOutboxEntity.builder()
                .id(reservationOutboxMessage.getId())
                .sagaId(reservationOutboxMessage.getSagaId())
                .createdAt(reservationOutboxMessage.getCreatedAt())
                .type(reservationOutboxMessage.getType())
                .payload(reservationOutboxMessage.getPayload())
                .orderStatus(reservationOutboxMessage.getOrderStatus())
                .sagaStatus(reservationOutboxMessage.getSagaStatus())
                .outboxStatus(reservationOutboxMessage.getOutboxStatus())
                .version(reservationOutboxMessage.getVersion())
                .build();
    }

    public ProductReservationOutboxMessage productReservationEntityToOutboxMessage(ProductReservationOutboxEntity
                                                                                               reservationOutboxEntity) {
        return ProductReservationOutboxMessage.builder()
                .id(reservationOutboxEntity.getId())
                .sagaId(reservationOutboxEntity.getSagaId())
                .createdAt(reservationOutboxEntity.getCreatedAt())
                .type(reservationOutboxEntity.getType())
                .payload(reservationOutboxEntity.getPayload())
                .orderStatus(reservationOutboxEntity.getOrderStatus())
                .sagaStatus(reservationOutboxEntity.getSagaStatus())
                .outboxStatus(reservationOutboxEntity.getOutboxStatus())
                .version(reservationOutboxEntity.getVersion())
                .build();
    }

}
