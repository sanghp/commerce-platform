package com.commerce.platform.product.service.domain;

import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.product.service.domain.valueobject.ProductReservationId;
import com.commerce.platform.product.service.domain.valueobject.SagaId;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.UUID;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.domain.valueobject.OrderId;

@Slf4j
public class ProductReservationDomainServiceImpl implements ProductReservationDomainService {

    @Override
    public ProductReservation createProductReservation(ProductId productId, OrderId orderId, SagaId sagaId,
                                                      Integer quantity, ZonedDateTime requestTime) {
        ProductReservation reservation = ProductReservation.builder()
                .productReservationId(new ProductReservationId(UUID.randomUUID()))
                .productId(productId)
                .orderId(orderId)
                .sagaId(sagaId)
                .quantity(quantity)
                .build();
        
        reservation.initializeReservation(requestTime);
        reservation.validateReservation();
        
        log.info("Product reservation is created with id: {} for order: {} at {}", 
                reservation.getId().getValue(), orderId.getValue(), requestTime);
        return reservation;
    }

    @Override
    public ProductReservation confirmProductReservation(ProductReservation reservation, ZonedDateTime requestTime) {
        reservation.confirm();
        log.info("Product reservation with id: {} is confirmed at {}", 
                reservation.getId().getValue(), requestTime);
        return reservation;
    }

    @Override
    public ProductReservation cancelProductReservation(ProductReservation reservation, ZonedDateTime requestTime) {
        reservation.cancel();
        log.info("Product reservation with id: {} is cancelled at {}", 
                reservation.getId().getValue(), requestTime);
        return reservation;
    }
} 